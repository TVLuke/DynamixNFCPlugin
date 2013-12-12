/*
 * Copyright (C) the Dynamix Framework Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Some RDF Extensions by LUkas Ruge
 */

package org.ambientdynamix.contextplugins.nfc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.ambientdynamix.api.application.IContextInfo;

import android.nfc.Tag;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Notes
 * Chunked: http://hi-android.info/src/android/nfc/NdefRecord.java.html
 * http://developer.android.com/resources/samples/NFCDemo/src/com/example/android/nfc/NdefMessageParser.html
 * http://developer.android.com/resources/samples/NFCDemo/src/com/example/android/nfc/index.html
 */
class NfcTag implements IContextInfo, INfcTag {
	public static Parcelable.Creator<NfcTag> CREATOR = new Parcelable.Creator<NfcTag>() {
		public NfcTag createFromParcel(Parcel in) {
			return new NfcTag(in);
		}

		public NfcTag[] newArray(int size) {
			return new NfcTag[size];
		}
	};
	// Sample context data
	private Tag nfcTag;

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTag#getNfcTag()
	 */
	public Tag getNfcTag() {
		return nfcTag;
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTag#getTagIdAsString()
	 */
	public String getTagIdAsString() 
	{
		return byteArrayToHexString(nfcTag.getId());
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	};

	public static String byteArrayToHexString(byte[] inarray) {
		int i, j, in;
		String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
		String out = "";
		for (j = 0; j < inarray.length; ++j) {
			in = (int) inarray[j] & 0xff;
			i = (in >> 4) & 0x0f;
			out += hex[i];
			i = in & 0x0f;
			out += hex[i];
		}
		return out;
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTag#getContextType()
	 */
	@Override
	public String getContextType() {
		return "org.ambientdynamix.contextplugins.nfc.tag";
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTag#getStringRepresentation(java.lang.String)
	 */
	@Override
	public String getStringRepresentation(String format) {
		if (format.equalsIgnoreCase("text/plain"))
		{
		return getTagIdAsString();
		}
		else if(format.equalsIgnoreCase("RDF/XML"))
		{
			String result="<rdf:RDF\n" +
					"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
					"xmlns:z.0=\"http://dynamix.org/semmodel/org.ambientdynamix.contextplugins.nfc/0.1/\"\n" +
					"xmlns:z.1=\"http://dynamix.org/semmodel/0.1/\" > \n";
			result=result+" <rdf:Description rdf:about=\"http://dynamix.org/semmodel/org.ambientdynamix.contextplugins.nfc/0.1/"+getTagIdAsString()+"\">\n";
			result=result+" <rdf:type>http://dynamix.org/semmodel/0.1/org.ambientdynamix.contextplugins.nfc.tag</rdf:type>\n";
			result=result+"<z.0:hasTagID>"+getTagIdAsString()+"</z.0:hasTagID>\n";
			result=result+"  </rdf:Description>\n </rdf:RDF>";
			return result;
		}
		else
		{
			return "";
		}
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTag#getImplementingClassname()
	 */
	@Override
	public String getImplementingClassname() {
		return this.getClass().getName();
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTag#getStringRepresentationFormats()
	 */
	@Override
	public Set<String> getStringRepresentationFormats() {
		Set<String> formats = new HashSet<String>();
		formats.add("text/plain");
		formats.add("RDF/XML");
		return formats;
	};

	public NfcTag(Tag nfcTag) {
		this.nfcTag = nfcTag;
	}

	private NfcTag(final Parcel in) {
		this.nfcTag = in.readParcelable(null);
	}

	public IBinder asBinder() {
		return null;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(this.nfcTag, 0);
	}
}
