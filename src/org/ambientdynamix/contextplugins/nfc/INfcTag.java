package org.ambientdynamix.contextplugins.nfc;

import java.util.Set;

import android.nfc.Tag;

public interface INfcTag {
	public Tag getNfcTag();

	public String getTagIdAsString();

	public String getContextType();

	public String getStringRepresentation(String format);

	public String getImplementingClassname();

	public Set<String> getStringRepresentationFormats();
}