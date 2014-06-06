package org.flowvisor.message.actions;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionEnqueue;

public class FVActionEnqueue extends OFActionEnqueue implements SlicableAction {

	@Override
	public void slice(List<OFAction> approvedActions, OFMatch match,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws ActionDisallowedException {
		
		/*
		 * Match OFMatch, if flowentry has queue id then OK. 
		 */
		/*
		FVMatch neoMatch = new FVMatch(match);
		neoMatch.setInputPort(this.port); // This causes a bug if flowspaces specify in_port. Enqueue action targets output ports.
                                            // Filtering flowspaces by the match /w in_port replaced by target output port doesn't make sense... it switches up src/dst
                                            // Fix isn't trivial; Cannot simply swap src/dst because FV we don't know what fields are specified...
                                            // Fix: Simply filter flowspace entries by output port; Less efficient though...
		FVLog.log(LogLevel.WARN, fvSlicer, "    IN ENQUEUE, MATCH IS: " + match + " and wildcards are: " + match.getWildcards());
		FVLog.log(LogLevel.WARN, fvSlicer, "    IN ENQUEUE, NEOMATCH IS: " + neoMatch + " and wildcards are: " + neoMatch.getWildcards());
		List<FlowEntry> entries = fvSlicer.getFlowSpace().matches(fvClassifier.getDPID(), neoMatch);
		*/
		FVMatch neoMatch = new FVMatch();
		neoMatch.setWildcards(neoMatch.getWildcards() & ~FVMatch.OFPFW_IN_PORT); // Only match is input port
		neoMatch.setInputPort(this.port);
		List<FlowEntry> entries = fvSlicer.getFlowSpace().matches(fvClassifier.getDPID(), neoMatch);
		for (FlowEntry fe : entries) {
			for (OFAction act : fe.getActionsList()) {
				SliceAction sa = (SliceAction) act;
				if (sa.getSliceName().equals(fvSlicer.getSliceName()) && 
						fe.getQueueId().contains(this.queueId)) {
					approvedActions.add(this);
					return;
				}
			}
		}
		//FVLog.log(LogLevel.WARN, fvSlicer, ".......... IN ENQUEUE... uh oh!... ");
		throw new ActionDisallowedException("Slice " + 
				fvSlicer.getSliceName() + " may not enqueue to queue " + this.queueId
				+ " for port " + this.port,
				OFBadActionCode.OFPBAC_BAD_QUEUE);
		
	}

}
