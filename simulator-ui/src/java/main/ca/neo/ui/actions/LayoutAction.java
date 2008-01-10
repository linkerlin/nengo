package ca.neo.ui.actions;

import ca.neo.ui.models.viewers.WorldLayout;
import ca.shu.ui.lib.actions.ActionException;
import ca.shu.ui.lib.actions.ReversableAction;
import ca.shu.ui.lib.world.World;
import ca.shu.ui.lib.world.WorldObject;

public abstract class LayoutAction extends ReversableAction {
	private static final long serialVersionUID = 1L;

	private WorldLayout savedLayout;

	World nodeViewer;

	public LayoutAction(World nodeViewer, String description, String actionName) {
		super(description, actionName);
		this.nodeViewer = nodeViewer;
	}

	@Override
	protected void action() throws ActionException {
		savedLayout = new WorldLayout("", nodeViewer, false);
		applyLayout();
	}

	protected abstract void applyLayout();

	protected void restoreNodePositions() {

		for (WorldObject wo : nodeViewer.getGround().getObjects()) {
			wo.setOffset(savedLayout.getPosition(wo));
		}
	}

	protected void setSavedLayout(WorldLayout savedLayout) {
		this.savedLayout = savedLayout;
	}

	@Override
	protected void undo() throws ActionException {
		restoreNodePositions();
	}

}
