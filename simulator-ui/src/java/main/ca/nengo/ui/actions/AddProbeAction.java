/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "AddProbeAction.java". Description:
"Action for adding probes

  @author Shu Wu"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU
Public License license (the GPL License), in which case the provisions of GPL
License are applicable  instead of those above. If you wish to allow use of your
version of this file only under the terms of the GPL License and not to allow
others to use your version of this file under the MPL, indicate your decision
by deleting the provisions above and replace  them with the notice and other
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
 */

package ca.nengo.ui.actions;

import java.util.Map.Entry;

import ca.nengo.model.SimulationException;
import ca.nengo.ui.lib.actions.ActionException;
import ca.nengo.ui.lib.actions.ReversableAction;
import ca.nengo.ui.models.UINeoNode;
import ca.nengo.ui.models.nodes.widgets.UIProbe;

/**
 * Action for adding probes
 * 
 * @author Shu Wu
 */
public class AddProbeAction extends ReversableAction {

    private static final long serialVersionUID = 1;

    private UIProbe probeCreated;

    private Entry<String, String> state;
    private UINeoNode myNode;

    /**
     * TODO
     * 
     * @param nodeParent TODO
     * @param state TODO
     */
    public AddProbeAction(UINeoNode nodeParent, Entry<String, String> state) {
        super(state.getKey() + " - " + state.getValue());
        this.state = state;
        this.myNode = nodeParent;

    }

    @Override
    protected void action() throws ActionException {

        try {
            probeCreated = myNode.addProbe(state.getKey());
        } catch (SimulationException e) {
            throw new ActionException("Probe could not be added: " + e.getMessage(), true, e);
        }
    }

    @Override
    protected void undo() throws ActionException {
        myNode.removeProbe(probeCreated);

    }

}