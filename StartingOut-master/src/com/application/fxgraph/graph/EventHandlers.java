package com.application.fxgraph.graph;

import com.application.Main;
import com.application.db.DAOImplementation.*;
import com.application.db.DatabaseUtil;
import com.application.db.TableNames;
import com.application.fxgraph.cells.CircleCell;
import com.application.logs.fileHandler.CallTraceLogFile;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.controlsfx.control.PopOver;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventHandlers {

    final DragContext dragContext = new DragContext();

    Graph graph;
    static Main main;

    public EventHandlers(Graph graph) {
        this.graph = graph;
    }

    public void makeDraggable(final Node node) {
        // node.addEventFilter(MouseEvent.ANY, onMouseHoverToShowInfoEventHandler);
        // node.addEventFilter(MouseEvent.ANY, event -> System.out.println(event));


        node.setOnMousePressed(onMousePressedToCollapseTree);
        node.setOnMouseEntered(onMouseHoverToShowInfoEventHandler);

        // *****************
        // To dismiss the pop over when cursor leaves the circle. But this makes it impossible to click buttons on pop
        // over because the pop over hides when the cursor is moved to click the button.
        // node.setOnMouseExited(onMouseExitToDismissPopover);
        // *****************

        // Original handlers
        // node.setOnMousePressed(onMousePressedEventHandler);
        // node.setOnMouseDragged(onMouseDraggedEventHandler);
        // node.setOnMouseReleased(onMouseReleasedEventHandler);
    }

    PopOver popOver;

    EventHandler<MouseEvent> onMouseHoverToShowInfoEventHandler = new EventHandler<MouseEvent>() {

        @Override
        public void handle(MouseEvent event) {

            Node node = (Node) event.getSource();
            CircleCell cell = (CircleCell) node;
            String timeStamp;
            int methodId, processId, threadId;
            String parameters, packageName = "", methodName = "", parameterTypes = "", eventType, lockObjectId = "";
            String sql = "";
            try (ResultSet callTraceRS = CallTraceDAOImpl.selectWhere("id = (Select id_enter_call_trace FROM " + TableNames.ELEMENT_TABLE +
                    " WHERE id = " + cell.getCellId() + ")")) {
                if (callTraceRS.next()) {
                    timeStamp = callTraceRS.getString("time_instant");
                    methodId = callTraceRS.getInt("method_id");
                    processId = callTraceRS.getInt("process_id");
                    threadId = callTraceRS.getInt("thread_id");
                    parameters = callTraceRS.getString("parameters");
                    eventType = callTraceRS.getString("message");
                    lockObjectId = callTraceRS.getString("lockobjid");
                    try (ResultSet methodDefRS = MethodDefnDAOImpl.selectWhere("id = " + methodId)) {
                        if (methodDefRS.next()) {
                            packageName = methodDefRS.getString("package_name");
                            methodName = methodDefRS.getString("method_name");
                            parameterTypes = methodDefRS.getString("parameter_types");
                        }

                        if (methodId == 0 ) {
                            methodName = eventType;
                            packageName = "N/A";
                            parameterTypes = "N/A";
                            parameters = "N/A";
                        }
                    } catch (SQLException e) {}

                    Label lMethodName = new Label(methodName);
                    Label lPackageName = new Label(packageName);
                    Label lParameterTypes = new Label(parameterTypes);
                    Label lParameters = new Label(parameters);
                    Label lProcessId = new Label(String.valueOf(processId));
                    Label lThreadId = new Label(String.valueOf(threadId));
                    Label lTimeInstant = new Label(timeStamp);

                    GridPane gridPane = new GridPane();
                    gridPane.setPadding(new Insets(10, 10, 10, 10));
                    gridPane.setVgap(10);
                    gridPane.setHgap(20);
                    gridPane.add(new Label("Method Name: "), 0, 0);
                    gridPane.add(lMethodName, 1, 0);

                    gridPane.add(new Label("Package Name: "), 0, 1);
                    gridPane.add(lPackageName, 1, 1);

                    gridPane.add(new Label("Parameter Types: "), 0, 2);
                    gridPane.add(lParameterTypes, 1, 2);

                    gridPane.add(new Label("Parameters: "), 0, 3);
                    gridPane.add(lParameters, 1, 3);

                    gridPane.add(new Label("Process ID: "), 0, 4);
                    gridPane.add(lProcessId, 1, 4);

                    gridPane.add(new Label("Thread ID: "), 0, 5);
                    gridPane.add(lThreadId, 1, 5);

                    gridPane.add(new Label("Time of Invocation: "), 0, 6);
                    gridPane.add(lTimeInstant, 1, 6);


                    /*
                    * wait-enter -> lock released.
                    *       Get all elements with same lock id and notify-enter
                    * wait-exit -> lock reacquired.
                    *
                    * notify-enter / notify-exit -> lock released
                    *
                    * object lock flow:
                    * wait-enter -> notify-enter / notify-exit -> wait-exit
                    * */

                    List<Integer> ctIdList = new ArrayList<>();
                    List<Integer> eleIdList = new ArrayList<>();

                    if (eventType.equalsIgnoreCase("WAIT-ENTER")) {
                        int ctId = -2;  // Will throw exception if value not changed. Which is what we want.
                        sql = "lockobjid = '" + lockObjectId + "'" +
                                " AND (message = 'NOTIFY-ENTER' OR message = 'NOTIFYALL-ENTER')" +
                                " AND time_instant >= " + "'" + timeStamp + "'";

                        try (ResultSet rs = CallTraceDAOImpl.selectWhere(sql)) {
                            if (rs.next()) {
                                ctId = rs.getInt("id");
                                ctIdList.add(ctId);
                            }
                        }

                        try (ResultSet elementRS = ElementDAOImpl.selectWhere("id_enter_call_trace = " + ctId)) {
                            // Expecting to see a single row.
                            if (elementRS.next()) {
                                int elementId = elementRS.getInt("id");
                                eleIdList.add(elementId);
                            }
                        }
                    } else if (eventType.equalsIgnoreCase("NOTIFY-ENTER")) {

                        try (Connection conn = DatabaseUtil.getConnection(); Statement ps = conn.createStatement()) {


                            sql = "SELECT * FROM " + TableNames.CALL_TRACE_TABLE + " AS parent\n" +
                                    "WHERE MESSAGE = 'WAIT-EXIT' \n" +
                                    "AND LOCKOBJID = '" + lockObjectId + "' " +
                                    "AND TIME_INSTANT >= '" + timeStamp + "' \n" +
                                    "AND (SELECT count(*) \n" +
                                    "FROM " + TableNames.CALL_TRACE_TABLE + " AS child \n" +
                                    "WHERE child.message = 'WAIT-ENTER' \n" +
                                    "AND LOCKOBJID = '" + lockObjectId + "' " +
                                    "AND child.TIME_INSTANT >=  '" + timeStamp + "' \n" +
                                    "AND child.TIME_INSTANT <= parent.time_instant\n" +
                                    ")\n" +
                                    "= 0\n";

                            System.out.println("Sql: " + sql);
                            int ctId = -2;
                            try (ResultSet resultSet = ps.executeQuery(sql)) {
                                if (resultSet.next()) {
                                    ctId = resultSet.getInt("id");
                                    ctIdList.add(ctId);
                                }
                            }

                            try (ResultSet elementRS = ElementDAOImpl.selectWhere("id_exit_call_trace = " + ctId)) {
                                // Expecting to see a single row.
                                if (elementRS.next()) {
                                    int elementId = elementRS.getInt("id");
                                    eleIdList.add(elementId);
                                }
                            }
                        }

                    } else if (eventType.equalsIgnoreCase("NOTIFYALL-ENTER")) {
                        try (Connection conn = DatabaseUtil.getConnection();
                             Statement ps = conn.createStatement();) {


                            sql = "SELECT * FROM " + TableNames.CALL_TRACE_TABLE + " AS parent WHERE MESSAGE = 'WAIT-EXIT' " +
                                    "AND LOCKOBJID = '" + lockObjectId + "' " +
                                    "AND TIME_INSTANT >= '" + timeStamp + "' " +
                                    "AND (SELECT count(*) FROM " + TableNames.CALL_TRACE_TABLE + " AS child " +
                                    "WHERE child.message = 'WAIT-ENTER' " +
                                    "AND LOCKOBJID = '" + lockObjectId + "' " +
                                    "AND child.TIME_INSTANT >= '" + timeStamp + "' " +
                                    "AND child.TIME_INSTANT <= parent.time_instant ) = 0";

                            int ctId = -2;

                            try (ResultSet resultSet = ps.executeQuery(sql)) {
                                while (resultSet.next()) {
                                    ctId = resultSet.getInt("id");
                                    ctIdList.add(ctId);
                                }
                            }

                            ctIdList.stream().forEach(id -> {
                                try (ResultSet elementRS = ElementDAOImpl.selectWhere("id_exit_call_trace = " + id)) {
                                    // Can be more than a single row.
                                    while (elementRS.next()) {
                                        int elementId = elementRS.getInt("id");
                                        eleIdList.add(elementId);
                                    }
                                } catch (SQLException e) {
                                }
                            });
                        }
                    }

                    List<Button> buttonList = new ArrayList<>();
                    eleIdList.stream().forEach(elementId ->{
                        String query = "SELECT E.ID AS EID, bound_box_x_coordinate, bound_box_y_coordinate, THREAD_ID " +
                                "FROM CALL_TRACE AS CT " +
                                "JOIN ELEMENT AS E ON CT.ID = E.ID_ENTER_CALL_TRACE " +
                                "WHERE E.ID = " + elementId;
                        try (ResultSet elementRS = DatabaseUtil.select(query)){
                        // try (ResultSet elementRS = ElementDAOImpl.selectWhere("id = " + elementId)){
                            if (elementRS.next()) {
                                int id = elementRS.getInt("EID");
                                String targetThreadId = String.valueOf(elementRS.getInt("thread_id"));
                                float xCoordinate = elementRS.getFloat("bound_box_x_coordinate");
                                float yCoordinate = elementRS.getFloat("bound_box_y_coordinate");
                                double width = graph.getScrollPane().getContent().getBoundsInLocal().getWidth();
                                double height = graph.getScrollPane().getContent().getBoundsInLocal().getHeight();

                                Button button = new Button();
                                button.setOnMouseClicked(event1 -> {
                                    main.showThread(targetThreadId);
                                    Main.makeSelection(targetThreadId);
                                });
                                buttonList.add(button);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });

                    String message = "", actionMsg = "";
                    switch (eventType.toUpperCase()) {
                        case "WAIT-ENTER":
                            message = "Wait method was invoked and therefore, \nthe lock on object ( object id = " + lockObjectId +") \nwas released and reaquired here.";
                            actionMsg = "Go to Notify or NotifyAll \nmethods invocations.";
                            break;

                        case "NOTIFY-ENTER":
                            message = "Notify method was invoked and therefore, \nthe lock on object ( object id = " + lockObjectId +") \nwas released here.";
                            actionMsg = "Go to wait \nmethods invocations.";
                            break;
                        case "NOTIFYALL-ENTER":
                            message = "NotifyAll method was invoked and therefore, \nthe lock on object ( object id = " + lockObjectId +") \nwas released here.";
                            actionMsg = "Go to wait \nmethods invocations.";
                            break;
                    }
                    Label labelMessage = new Label(message);
                    labelMessage.setWrapText(true);

                    Label labelActionMsg = new Label(actionMsg);

                    gridPane.add(labelMessage, 0 , 7);
                    gridPane.add(labelActionMsg, 0 , 8);
                    int rowIndex = 8;
                    for (Button button: buttonList) {
                        button.setText("Goto node");
                        gridPane.add(button, 1, rowIndex++ );
                    }

                    popOver = new PopOver(gridPane);
                    popOver.setAnimated(true);
                    // popOver.detach();
                    popOver.setAutoHide(true);
                    popOver.show(node);
                }
            } catch (SQLException e) {
                System.out.println("Line that threw exception: " + sql);
                e.printStackTrace();
            }
        }
    };

    EventHandler<MouseEvent> onMouseExitToDismissPopover = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            if (popOver != null)
            popOver.hide();
        }
    };

    EventHandler<MouseEvent> onMousePressedToCollapseTree = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            CellLayer cellLayer = (CellLayer) graph.getCellLayer();
            CircleCell cell = (CircleCell) event.getSource();
            String cellId =  cell.getCellId();
            int collapsed = 0;
            try (ResultSet cellRS = ElementDAOImpl.selectWhere("id = " + cellId)) {
                if (cellRS.next()) {
                    collapsed = cellRS.getInt("collapsed");
                }
            } catch (SQLException e) {}

            /*
             * collapsed - actions
             *     0     - Show cell on UI. Starting value for all cells.
             *     1     - parent of this cell was minimized. Don't show on UI
             *     2     - this cell was minimized. Show on UI. Don't show children on UI.
             *     3     - parent of this cell was minimized. This cell was also minimized. Don't expand this cell's children. Don't show on UI.
             */
            if (collapsed == 1) {
                // expand sub tree.
                // System.out.println("onMousePressedToCollapseTree: cell: " + cellId + " ; collapsed: " + collapsed);
            } else if (collapsed == 0) {
                // Minimize now.
                // System.out.println(">>>> clicked on a collapsed = 0  cell.");
                if (cell != null)
                    ((Circle)cell.getChildren().get(0)).setFill(Color.BLUE);

                // ((Circle) ( (Group)cell.getView() )
                //             .getChildren().get(0))
                //             .setFill(Color.BLUE);
                // cell.getChildren().get(0).setStyle("-fx-background-color: blue");
                // cell.setStyle("-fx-background-color: blue");
                cell.setLabel("+");
                ElementDAOImpl.updateWhere("collapsed", "2", "id = " + cellId);
                Map<String, CircleCell> mapCircleCellsOnUI = graph.getModel().getMapCircleCellsOnUI();
                List<CircleCell> listCircleCellsOnUI = graph.getModel().getListCircleCellsOnUI();
                List<String> removeCircleCells = new ArrayList<>();

                Map<String, Edge> mapEdgesOnUI = graph.getModel().getMapEdgesOnUI();
                List<Edge> listEdgesOnUI = graph.getModel().getListEdgesOnUI();
                List<String> removeEdges = new ArrayList<>();

                recursivelyRemove(cellId, removeCircleCells, removeEdges);
                removeCircleCells.forEach(circleCellId -> {
                    if (mapCircleCellsOnUI.containsKey(circleCellId)) {
                        CircleCell circleCell = mapCircleCellsOnUI.get(circleCellId);
                        cellLayer.getChildren().remove(circleCell);
                        mapCircleCellsOnUI.remove(circleCellId);
                        listCircleCellsOnUI.remove(circleCell);
                    }
                });
                // listEdgesOnUI.forEach(edge -> System.out.print(" : " + edge));
                // System.out.println();

                removeEdges.forEach(edgeId -> {
                    if (mapEdgesOnUI.containsKey(edgeId)) {
                        Edge edge = mapEdgesOnUI.get(edgeId);
                        cellLayer.getChildren().remove(edge);
                        mapEdgesOnUI.remove(edgeId);
                        listEdgesOnUI.remove(edge);
                    }
                });
                // listEdgesOnUI.forEach(edge -> System.out.print(" : " + edge));
                // System.out.println();

            } else if (collapsed == 2) {
                if (cell != null)
                    ((Circle)cell.getChildren().get(0)).setFill(Color.RED);
                // ( (Circle) ( (Groupq)cell.getView() ).getChildren().get(0) ).setFill(Color.RED);
                cell.setLabel("-");
                recursivelyAdd(cellId);
            } else if (collapsed == 3) {
                System.out.println("onMousePressedToCollapseTree: cell: " + cellId + " ; collapsed: " + collapsed);
                throw new IllegalStateException("This cell should not have been on the UI.");
            }
        }
    };

    public void recursivelyAdd(String cellId) {

        try (ResultSet elementRS = ElementDAOImpl.selectWhere("id = " + cellId)) {
            if (elementRS.next()) {
                int collapsed = elementRS.getInt("collapsed");
                if (collapsed == 0) {
                    throw new IllegalStateException("Collapsed cannot be 0 here.");
                } else if (collapsed == 1) {
                    ElementDAOImpl.updateWhere("collapsed", "0", "id = " + cellId);
                    float xCoordinateTemp = elementRS.getFloat("bound_box_x_coordinate");
                    float yCoordinateTemp = elementRS.getFloat("bound_box_y_coordinate");
                    CircleCell cell = new CircleCell(cellId, xCoordinateTemp, yCoordinateTemp);
                    graph.getModel().addCell(cell);

                    try (ResultSet parentRS = ElementToChildDAOImpl.selectWhere("child_id = " + cellId)) {
                        if (parentRS.next()) {
                            String parentId = String.valueOf(parentRS.getInt("parent_id"));
                            CircleCell parentCell = graph.getModel().getMapCircleCellsOnUI().get(parentId);

                            Edge edge = new Edge(parentCell, cell);

                            EdgeDAOImpl.updateWhere("collapsed", "0",
                                    "fk_target_element_id = " + cellId);

                            graph.getModel().addEdge(edge);

                        }
                    }
                    graph.myEndUpdate();

                    try (ResultSet childrenRS = ElementToChildDAOImpl.selectWhere("parent_id = " + cellId)) {
                        while (childrenRS.next()) {
                            String childId = String.valueOf(childrenRS.getInt("child_id"));
                            recursivelyAdd(childId);
                        }
                    }

                } else if (collapsed == 2) {
                    // update collapsed=0
                    ElementDAOImpl.updateWhere("collapsed", "0", "id = " + cellId);
                    // for all children with collapsed=1, show and update collapsed=0
                    try (ResultSet childrenRS = ElementToChildDAOImpl.selectWhere("parent_id = " + cellId)) {
                        while (childrenRS.next()) {
                            String childId = String.valueOf(childrenRS.getInt("child_id"));
                            recursivelyAdd(childId);
                        }
                    }

                } else if (collapsed == 3) {
                    ElementDAOImpl.updateWhere("collapsed", "2", "id = " + cellId);
                    float xCoordinateTemp = elementRS.getFloat("bound_box_x_coordinate");
                    float yCoordinateTemp = elementRS.getFloat("bound_box_y_coordinate");
                    CircleCell cell = new CircleCell(cellId, xCoordinateTemp, yCoordinateTemp);
                    graph.getModel().addCell(cell);

                    try (ResultSet parentRS = ElementToChildDAOImpl.selectWhere("child_id = " + cellId)) {
                        if (parentRS.next()) {
                            String parentId = String.valueOf(parentRS.getInt("parent_id"));
                            CircleCell parentCell = graph.getModel().getMapCircleCellsOnUI().get(parentId);

                            Edge edge = new Edge(parentCell, cell);
                            EdgeDAOImpl.updateWhere("collapsed", "0",
                                    "fk_target_element_id = " + cellId);
                            graph.getModel().addEdge(edge);
                        }
                    }
                    graph.myEndUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recursivelyRemove(String cellId, List<String> removeCircleCells, List<String> removeEdges ) {
        try (ResultSet childrenRS = ElementToChildDAOImpl.selectWhere("parent_id = " + cellId)) {
            try {
                while (childrenRS.next()) {
                    String childId = String.valueOf(childrenRS.getInt("child_id"));
                    removeCircleCells.add(childId);
                    removeEdges.add(childId);

                    ElementDAOImpl.updateWhere("collapsed", "1",
                            "id = " + childId + " AND collapsed = 0");
                    ElementDAOImpl.updateWhere("collapsed", "3",
                            "id = " + childId + " AND collapsed = 2");

                    EdgeDAOImpl.updateWhere("collapsed", "1",
                            "fk_target_element_id = " + childId);

                    recursivelyRemove(childId, removeCircleCells, removeEdges);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {}
    }

    @SuppressWarnings("unused")
    EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            Node node = (Node) event.getSource();
            double scale = graph.getScale();
            dragContext.x = node.getBoundsInParent().getMinX() * scale - event.getScreenX();
            dragContext.y = node.getBoundsInParent().getMinY()  * scale - event.getScreenY();
        }
    };

    EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            Node node = (Node) event.getSource();
            double offsetX = event.getScreenX() + dragContext.x;
            double offsetY = event.getScreenY() + dragContext.y;
            // adjust the offset in case we are zoomed
            double scale = graph.getScale();
            offsetX /= scale;
            offsetY /= scale;
            node.relocate(offsetX, offsetY);
        }
    };

    EventHandler<MouseEvent> onMouseReleasedEventHandler = event -> {};

    class DragContext {
        double x;
        double y;
    }

    public static void saveRef(Main m) {
        main = m;
    }
}