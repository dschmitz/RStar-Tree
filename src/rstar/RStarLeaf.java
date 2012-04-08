package rstar;

import rstar.dto.NodeDTO;
import rstar.interfaces.IRStarNode;
import rstar.spatial.HyperRectangle;
import rstar.spatial.SpatialPoint;
import util.Constants;

import java.util.ArrayList;

/**
 * User: Lokesh
 * Date: 3/4/12
 * Time: 2:22 AM
 */
public class RStarLeaf extends RStarNode {
    public ArrayList<SpatialPoint> children;

    public RStarLeaf(int dimension) {
        createId();
        _dimension = dimension;
        children = new ArrayList<SpatialPoint>(CAPACITY);
        childPointers = new long[Constants.MAX_CHILDREN];
        mbr = new HyperRectangle(dimension);
    }

    public RStarLeaf(NodeDTO dto, long nodeId) {
        this.nodeId = nodeId;
        this.childPointers = dto.children;
        // TODO mbr
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isNotFull() {
        return (children.size() < CAPACITY);
    }

    @Override
    public <T> int insert(T newChild) {
        if (this.isNotFull() && (newChild instanceof SpatialPoint)) {
            children.add((SpatialPoint) newChild);
            mbr.update((SpatialPoint) newChild);
            return 1;
        } else return -1;
    }

    @Override
    public HyperRectangle getMBR() {
        return mbr;
    }

    @Override
    public long getNodeId() {
        createId();
        return nodeId;
    }

    @Override
    public ArrayList<SpatialPoint> getOverlappingChildren(HyperRectangle searchRegion) {
        HyperRectangle intersection = mbr.getIntersection(searchRegion);
        return pointsInRegion(intersection);
    }

    private ArrayList<SpatialPoint> pointsInRegion(HyperRectangle region) {
        //TODO
        return children;
    }

    @Override
    public NodeDTO toDTO() {
        return new NodeDTO(childPointers, mbr.toDTO(), true);
    }

    public boolean hasUnsavedPoints(){
        return children.size() > childPointers.length;
    }
    public int indexOfFirstUnsavedPoint(){
        return (children.size() - childPointers.length) - 1;
    }
}
