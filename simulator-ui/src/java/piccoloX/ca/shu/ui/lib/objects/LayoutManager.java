package ca.shu.ui.lib.objects;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

public class LayoutManager implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Point2D currentPosition;

	PBounds layoutBounds;

	double horizontalPadding = 0;

	double verticalPadding = 0;
	
	boolean firstLayout;

	enum LayoutStyle {
		HORIZONTAL, VERTICAL
	};

	LayoutStyle style = LayoutStyle.VERTICAL;

	public LayoutManager() {
		
		setLeftPadding(10);
		setVerticalPadding(10);
		reset();
	}

	public void setStyle(LayoutStyle style) {
		this.style = style;

	}

	public PBounds getBounds() {
		return layoutBounds;
	}

	public Point2D getPoint() {
		return currentPosition;
	}

	public double getVerticalPadding() {
		return verticalPadding;
	}

	public double getX() {
		return currentPosition.getX();
	}

	public double getY() {
		return currentPosition.getY();
	}

	public void positionNode(PNode node) {
		PBounds nodeBounds = new PBounds(node.localToParent(node.getBounds()));

		if (firstLayout) {
			firstLayout = false;
			translate(horizontalPadding, verticalPadding);
		}
		
		// translate(leftPadding, verticalPadding);
		node.setOffset(currentPosition);
		if (style == LayoutStyle.VERTICAL) {
			translate(0, node.getHeight() + verticalPadding);
		} else if (style == LayoutStyle.HORIZONTAL) {
			translate(node.getWidth() + horizontalPadding, 0);
		}

		// updates the bound of children who have been layed out
		// PBounds nodeBounds = node.getBounds();
		nodeBounds.x = nodeBounds.getX() + node.getOffset().getX()
				+ horizontalPadding;
		nodeBounds.y = nodeBounds.getY() + node.getOffset().getY()
				+ verticalPadding;
		Rectangle2D.union(layoutBounds, nodeBounds, layoutBounds);

	}

	public void reset() {
		currentPosition = new Point2D.Double(0, 0);
		layoutBounds = new PBounds(0, 0, 1, 1);
		firstLayout = true;
	}

	public void setLeftPadding(double leftPadding) {
		this.horizontalPadding = leftPadding;
	}

	public void setVerticalPadding(double verticalPadding) {
		this.verticalPadding = verticalPadding;
	}

	public void translate(double dX, double dY) {
		
		currentPosition.setLocation(currentPosition.getX() + dX,
				currentPosition.getY() + dY);
	}

}