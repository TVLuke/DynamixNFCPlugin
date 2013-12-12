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
package org.ambientdynamix.contextplugins.nfc;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.ambientdynamix.api.application.IContextInfo;

import android.nfc.NdefRecord;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Adapted from http://developer.android.com/resources/samples/NFCDemo/src/com/example/android/nfc/record/TextRecord.html
 */
class NfcTextRecord implements IContextInfo, ParsedNdefRecord, INfcTextRecord {
	public static Parcelable.Creator<NfcTextRecord> CREATOR = new Parcelable.Creator<NfcTextRecord>() {
		public NfcTextRecord createFromParcel(Parcel in) {
			return new NfcTextRecord(in);
		}

		public NfcTextRecord[] newArray(int size) {
			return new NfcTextRecord[size];
		}
	};
	private String mLanguageCode = "";
	private String mText = "";
	private NdefRecord record;

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTextRecord#getNdefRecord()
	 */
	public NdefRecord getNdefRecord() {
		return this.record;
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTextRecord#getText()
	 */
	public String getText() {
		return mText;
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTextRecord#getLanguageCode()
	 */
	public String getLanguageCode() {
		return mLanguageCode;
	}

	// TODO: deal with text fields which span multiple NdefRecords
	public static NfcTextRecord parse(NdefRecord record) throws Exception {
		if (record.getTnf() != NdefRecord.TNF_WELL_KNOWN)
			throw new Exception();
		if (record.getType() != NdefRecord.RTD_TEXT)
			throw new Exception();
		try {
			byte[] payload = record.getPayload();
			/*
			 * payload[0] contains the "Status Byte Encodings" field, per the NFC Forum "Text Record Type Definition"
			 * section 3.2.1. bit7 is the Text Encoding Field. if (Bit_7 == 0): The text is encoded in UTF-8 if (Bit_7
			 * == 1): The text is encoded in UTF16 Bit_6 is reserved for future use and must be set to zero. Bits 5 to 0
			 * are the length of the IANA language code.
			 */
			String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
			int languageCodeLength = payload[0] & 0077;
			String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
			String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1,
					textEncoding);
			return new NfcTextRecord(record, languageCode, text);
		} catch (UnsupportedEncodingException e) {
			// should never happen unless we get a malformed tag.
			throw new IllegalArgumentException(e);
		}
	}

	public static boolean isText(NdefRecord record) {
		try {
			parse(record);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private NfcTextRecord(NdefRecord record, String languageCode, String text) {
		this.record = record;
		mLanguageCode = languageCode;
		mText = text;
	}

	private NfcTextRecord(final Parcel in) {
		this.record = in.readParcelable(null);
		this.mLanguageCode = in.readString();
		this.mText = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(this.record, flags);
		dest.writeString(this.mLanguageCode);
		dest.writeString(this.mText);
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTextRecord#getContextType()
	 */
	@Override
	public String getContextType() {
		return "org.ambientdynamix.contextplugins.nfc.text_record";
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTextRecord#getImplementingClassname()
	 */
	@Override
	public String getImplementingClassname() {
		return this.getClass().getName();
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTextRecord#getStringRepresentation(java.lang.String)
	 */
	@Override
	public String getStringRepresentation(String format) 
	{
		if (format.equalsIgnoreCase("text/plain"))
		{
			return mText+"@"+mLanguageCode;
		}
		else if(format.equalsIgnoreCase("RDF/XML"))
		{
			String result="<rdf:RDF\n" +
					"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
					"xmlns:z.0=\"http://dynamix.org/semmodel/org.ambientdynamix.contextplugins.nfc/0.1/\"\n" +
					"xmlns:z.1=\"http://dynamix.org/semmodel/0.1/\" > \n";
			result=result+" <rdf:Description rdf:about=\"http://dynamix.org/semmodel/org.ambientdynamix.contextplugins.nfc/0.1/"+mText+"\">\n";
			result=result+" <rdf:type>http://dynamix.org/semmodel/0.1/org.ambientdynamix.contextplugins.nfc.text_record</rdf:type>\n";
			result=result+"<z.0:hasText>"+mText+"</z.0:hasText>\n" +
					" <z.0:hasLanguage>"+mLanguageCode+"</z.0:hasLanguage>\n";
			result=result+"  </rdf:Description>\n </rdf:RDF>";
			return result;
		}
			return "";
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcTextRecord#getStringRepresentationFormats()
	 */
	@Override
	public Set<String> getStringRepresentationFormats() {
		Set<String> formats = new HashSet<String>();
		formats.add("text/plain");
		formats.add("RDF/XML");
		return formats;
	}
}
