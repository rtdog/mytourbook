package net.tourbook.importdata;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ActionHandlerImportFromDeviceDirect extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		RawDataManager.getInstance().executeImportFromDeviceDirect();

		return null;
	}

}
