package org.ambientdynamix.contextplugins.nfc;

import java.util.Set;

import android.nfc.NdefRecord;

public interface INfcTextRecord {
	public NdefRecord getNdefRecord();

	public String getText();

	/**
	 * Returns the ISO/IANA language code associated with this text element.
	 */
	public String getLanguageCode();

	public String getContextType();

	public String getImplementingClassname();

	public String getStringRepresentation(String format);

	public Set<String> getStringRepresentationFormats();
}