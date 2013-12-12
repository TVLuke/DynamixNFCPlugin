package org.ambientdynamix.contextplugins.nfc;

import java.util.Set;

import android.net.Uri;
import android.nfc.NdefRecord;

public interface INfcUriRecord {
	public abstract NdefRecord getNdefRecord();

	public abstract Uri getUri();

	public abstract String getContextType();

	public abstract String getStringRepresentation(String format);

	public abstract String getImplementingClassname();

	public abstract Set<String> getStringRepresentationFormats();
}