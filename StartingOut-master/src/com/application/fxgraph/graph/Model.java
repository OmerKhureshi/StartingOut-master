package com.application.fxgraph.graph;

import java.util.*;

import com.application.Main;
import com.application.fxgraph.ElementHelpers.Element;
import com.application.fxgraph.cells.CircleCell;
import com.application.fxgraph.cells.TriangleCell;
import com.application.fxgraph.cells.RectangleCell;

public class Model {

    Cell graphParent;

    List<Cell> allCells;
    List<Cell> addedCells;
    List<Cell> removedCells;

    List<Edge> allEdges;
    List<Edge> addedEdges;
    List<Edge> removedEdges;

    Map<String, Cell> cellMap; // <id,cell>

    Map<String, CircleCell> mapCircleCellsOnUI = new HashMap<>();

    List<CircleCell> listCircleCellsOnUI = new ArrayList<>();

    Map<String, Edge> mapEdgesOnUI = new HashMap<>();

    List<Edge> listEdgesOnUI = new ArrayList<>();

    public Model() {
        graphParent = new Cell("_ROOT_");
        // clear model, create lists
        clear();
    }

    /*
     * new/modified methods start
     */
    public void addCell(CircleCell circleCell) {
        // circleCell.toFront();
        // circleCell.setTranslateZ(10);
        // synchronized (Main.getLock()) {
            if (!mapCircleCellsOnUI.containsKey(circleCell.getCellId())) {
                mapCircleCellsOnUI.put(circleCell.getCellId(), circleCell);
                listCircleCellsOnUI.add(circleCell);
            }
        // }
    }

    public void addEdge(Edge edge) {
        // edge.toBack();
        // edge.setTranslateZ(.5);
        addedEdges.add(edge);
        if (edge != null && !mapEdgesOnUI.containsKey(edge.getEdgeId())) {
            // System.out.println();
            // System.out.println();
            // System.out.println(">>>> Adding edge: " + edge.getEdgeId());
            // System.out.println(">>>> List: Before adding: size:" + listEdgesOnUI.size());
            // listEdgesOnUI.stream()
            //         .forEach(s -> System.out.print("    : " +s.getEdgeId()));
            // System.out.println();
            //
            // System.out.println(">>>> MAP: Before adding: size:" + mapEdgesOnUI.size());
            // mapEdgesOnUI.entrySet().stream()
            //         .forEach(s -> System.out.print("    : " + s.getKey()));
            // System.out.println();
            //**********************************************
            mapEdgesOnUI.put(
                    edge.getEdgeId(),
                    edge);
            listEdgesOnUI.add(edge);
            //**********************************************

            // System.out.println(">>>> List: After adding: size:" + listEdgesOnUI.size());
            // listEdgesOnUI.stream()
            //         .forEach(s -> System.out.print("    : " + s.getEdgeId()));
            // System.out.println();
            // System.out.println(">>>> MAP: After adding: size:" + mapEdgesOnUI.size());
            // mapEdgesOnUI.entrySet().stream()
            //         .forEach(s -> System.out.print("    : " + s.getKey()));
            // System.out.println();
        }
    }

    public void clearListCircleCellsOnUI() {
        listCircleCellsOnUI.clear();
    }

    public void clearListEdgesOnUI() {
        listEdgesOnUI.clear();
    }

    public List<CircleCell> getListCircleCellsOnUI() {
        return listCircleCellsOnUI;
    }
    public List<Edge> getListEdgesOnUI() {
        return listEdgesOnUI;
    }

    public Map<String, CircleCell> getMapCircleCellsOnUI() {
        return mapCircleCellsOnUI;
    }

    public Map<String, Edge> getMapEdgesOnUI() {
        return mapEdgesOnUI;
    }

    public void clearScrollPane() {
        mapCircleCellsOnUI.clear();
        listCircleCellsOnUI.clear();

        mapEdgesOnUI.clear();
        listEdgesOnUI.clear();
    }

    /*
    * new methods end
    * */

    public void clear() {

        allCells = new ArrayList<>();
        addedCells = new ArrayList<>();
        removedCells = new ArrayList<>();

        allEdges = new ArrayList<>();
        addedEdges = new ArrayList<>();
        removedEdges = new ArrayList<>();

        cellMap = new HashMap<>(); // <id,cell>

    }

    public void clearAddedLists() {
        addedCells.clear();
        addedEdges.clear();
    }

    public List<Cell> getAddedCells() {
        return addedCells;
    }

    public List<Cell> getRemovedCells() {
        return removedCells;
    }

    public List<Cell> getAllCells() {
        return allCells;
    }

    public List<Edge> getAddedEdges() {
        return addedEdges;
    }

    public List<Edge> getRemovedEdges() {
        return removedEdges;
    }

    public List<Edge> getAllEdges() {
        return allEdges;
    }

    public void addCell(String id, CellType type) {

        switch (type) {

            case RECTANGLE:
                RectangleCell rectangleCell = new RectangleCell(id);
                addCell(rectangleCell);
                break;

            case TRIANGLE:
                TriangleCell triangleCell = new TriangleCell(id);
                addCell(triangleCell);
                break;

            case CIRCLE:
                CircleCell circleCell = new CircleCell(id);
                addCell(circleCell);
                break;

            default:
                throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    public CircleCell addCircleCell(String id, Element element) {
        CircleCell circleCell = new CircleCell(id, element);
        element.setCircleCell(circleCell);
        addCell(circleCell);
        return circleCell;
    }

    public void addCell(Cell cell) {
        addedCells.add(cell);
        cellMap.put(cell.getCellId(), cell);
    }

    public void addEdge(CircleCell sourceCell, CircleCell targetCell) {
        Edge edge = new Edge(sourceCell, targetCell);
        addedEdges.add(edge);
    }

    public void addEdge(String sourceId, String targetId) {

        Cell sourceCell = cellMap.get(sourceId);
        Cell targetCell = cellMap.get(targetId);

        Edge edge = new Edge(sourceCell, targetCell);

        addedEdges.add(edge);

    }

    /**
     * Attach all cells which don't have a parent to graphParent
     *
     * @param cellList
     */
    public void attachOrphansToGraphParent(List<Cell> cellList) {

        for (Cell cell : cellList) {
            if (cell.getCellParents().size() == 0) {
                graphParent.addCellChild(cell);
            }
        }
    }

    /**
     * Remove the graphParent reference if it is set
     *
     * @param cellList
     */
    public void disconnectFromGraphParent(List<Cell> cellList) {

        for (Cell cell : cellList) {
            graphParent.removeCellChild(cell);
        }
    }

    public void merge() {

        // cells
        allCells.addAll(addedCells);
        allCells.removeAll(removedCells);

        addedCells.clear();
        removedCells.clear();

        // edges
        allEdges.addAll(addedEdges);
        allEdges.removeAll(removedEdges);

        addedEdges.clear();
        removedEdges.clear();

    }

}