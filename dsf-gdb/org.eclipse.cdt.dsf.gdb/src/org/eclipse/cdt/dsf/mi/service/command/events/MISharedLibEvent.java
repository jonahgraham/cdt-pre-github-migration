/*******************************************************************************
 * Copyright (c) 2009 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Wind River Systems   - Modified for new DSF Reference Implementation
 *******************************************************************************/

package org.eclipse.cdt.dsf.mi.service.command.events;

import org.eclipse.cdt.dsf.concurrent.Immutable;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.command.output.MIFrame;
import org.eclipse.cdt.dsf.mi.service.command.output.MIResult;

/**
 *
 */
@Immutable
public class MISharedLibEvent extends MIStoppedEvent {

    protected MISharedLibEvent(IExecutionDMContext ctx, int token, MIResult[] results, MIFrame frame) {
        super(ctx, token, results, frame);
    }

    public static MIStoppedEvent parse(IExecutionDMContext dmc, int token, MIResult[] results) 
    {
       MIStoppedEvent stoppedEvent = MIStoppedEvent.parse(dmc, token, results); 
       return new MISharedLibEvent(stoppedEvent.getDMContext(), token, results, stoppedEvent.getFrame());
    }
}
