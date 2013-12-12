package org.ambientdynamix.contextplugins.nfc;

import java.util.Set;

public interface INfcSmartPoster {
	public INfcUriRecord getNfcUriRecord();

	/**
	 * Returns the title of the smart poster. This may be {@code null}.
	 */
	public INfcTextRecord getTitle();

	public String getContextType();

	public String getImplementingClassname();

	public String getStringRepresentation(String format);

	public Set<String> getStringRepresentationFormats();
}