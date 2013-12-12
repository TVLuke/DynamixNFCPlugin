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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ambientdynamix.api.application.IContextInfo;

import android.net.Uri;
import android.nfc.NdefRecord;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Adapted from http://developer.android.com/resources/samples/NFCDemo/src/com/example/android/nfc/record/UriRecord.html
 */
class NfcUriRecord implements IContextInfo, ParsedNdefRecord, INfcUriRecord {
	/**
	 * NFC Forum "URI Record Type Definition" This is a mapping of "URI Identifier Codes" to URI string prefixes, per
	 * section 3.2.2 of the NFC Forum URI Record Type Definition document.
	 */
	private static final Map<Byte, String> URI_PREFIX_MAP = new HashMap<Byte, String>();
	static {
		URI_PREFIX_MAP.put((byte) 0x00, "");
		URI_PREFIX_MAP.put((byte) 0x01, "http://www.");
		URI_PREFIX_MAP.put((byte) 0x02, "https://www.");
		URI_PREFIX_MAP.put((byte) 0x03, "http://");
		URI_PREFIX_MAP.put((byte) 0x04, "https://");
		URI_PREFIX_MAP.put((byte) 0x05, "tel:");
		URI_PREFIX_MAP.put((byte) 0x06, "mailto:");
		URI_PREFIX_MAP.put((byte) 0x07, "ftp://anonymous:anonymous@");
		URI_PREFIX_MAP.put((byte) 0x08, "ftp://ftp.");
		URI_PREFIX_MAP.put((byte) 0x09, "ftps://");
		URI_PREFIX_MAP.put((byte) 0x0A, "sftp://");
		URI_PREFIX_MAP.put((byte) 0x0B, "smb://");
		URI_PREFIX_MAP.put((byte) 0x0C, "nfs://");
		URI_PREFIX_MAP.put((byte) 0x0D, "ftp://");
		URI_PREFIX_MAP.put((byte) 0x0E, "dav://");
		URI_PREFIX_MAP.put((byte) 0x0F, "news:");
		URI_PREFIX_MAP.put((byte) 0x10, "telnet://");
		URI_PREFIX_MAP.put((byte) 0x11, "imap:");
		URI_PREFIX_MAP.put((byte) 0x12, "rtsp://");
		URI_PREFIX_MAP.put((byte) 0x13, "urn:");
		URI_PREFIX_MAP.put((byte) 0x14, "pop:");
		URI_PREFIX_MAP.put((byte) 0x15, "sip:");
		URI_PREFIX_MAP.put((byte) 0x16, "sips:");
		URI_PREFIX_MAP.put((byte) 0x17, "tftp:");
		URI_PREFIX_MAP.put((byte) 0x18, "btspp://");
		URI_PREFIX_MAP.put((byte) 0x19, "btl2cap://");
		URI_PREFIX_MAP.put((byte) 0x1A, "btgoep://");
		URI_PREFIX_MAP.put((byte) 0x1B, "tcpobex://");
		URI_PREFIX_MAP.put((byte) 0x1C, "irdaobex://");
		URI_PREFIX_MAP.put((byte) 0x1D, "file://");
		URI_PREFIX_MAP.put((byte) 0x1E, "urn:epc:id:");
		URI_PREFIX_MAP.put((byte) 0x1F, "urn:epc:tag:");
		URI_PREFIX_MAP.put((byte) 0x20, "urn:epc:pat:");
		URI_PREFIX_MAP.put((byte) 0x21, "urn:epc:raw:");
		URI_PREFIX_MAP.put((byte) 0x22, "urn:epc:");
		URI_PREFIX_MAP.put((byte) 0x23, "urn:nfc:");
	}
	public static Parcelable.Creator<NfcUriRecord> CREATOR = new Parcelable.Creator<NfcUriRecord>() {
		public NfcUriRecord createFromParcel(Parcel in) {
			return new NfcUriRecord(in);
		}

		public NfcUriRecord[] newArray(int size) {
			return new NfcUriRecord[size];
		}
	};
	private final Uri mUri;
	private NdefRecord record;

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcUriRecord#getNdefRecord()
	 */
	public NdefRecord getNdefRecord() {
		return this.record;
	}

	private NfcUriRecord(NdefRecord record, Uri uri) {
		this.record = record;
		this.mUri = uri;
	}

	private NfcUriRecord(final Parcel in) {
		this.record = in.readParcelable(null);
		this.mUri = in.readParcelable(null);
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcUriRecord#getUri()
	 */
	public Uri getUri() {
		return mUri;
	}

	/**
	 * Convert {@link android.nfc.NdefRecord} into a {@link android.net.Uri}. This will handle both TNF_WELL_KNOWN /
	 * RTD_URI and TNF_ABSOLUTE_URI.
	 * 
	 * @throws IllegalArgumentException
	 *             if the NdefRecord is not a record containing a URI.
	 */
	public static NfcUriRecord parse(NdefRecord record) {
		short tnf = record.getTnf();
		if (tnf == NdefRecord.TNF_WELL_KNOWN) {
			return parseWellKnown(record);
		} else if (tnf == NdefRecord.TNF_ABSOLUTE_URI) {
			return parseAbsolute(record);
		}
		throw new IllegalArgumentException("Unknown TNF " + tnf);
	}

	/** Parse and absolute URI record */
	private static NfcUriRecord parseAbsolute(NdefRecord record) {
		byte[] payload = record.getPayload();
		Uri uri = Uri.parse(new String(payload, Charset.forName("UTF-8")));
		return new NfcUriRecord(record, uri);
	}

	/** Parse an well known URI record */
	private static NfcUriRecord parseWellKnown(NdefRecord record) {
		byte[] payload = record.getPayload();
		/*
		 * payload[0] contains the URI Identifier Code, per the NFC Forum "URI Record Type Definition" section 3.2.2.
		 * payload[1]...payload[payload.length - 1] contains the rest of the URI.
		 */
		String prefix = URI_PREFIX_MAP.get(payload[0]);
		byte[] schemeBytes = prefix.getBytes(Charset.forName("UTF-8"));
		byte[] uriBytes = Arrays.copyOfRange(payload, 1, payload.length);
		byte[] fullUri = new byte[schemeBytes.length + uriBytes.length];
		System.arraycopy(schemeBytes, 0, fullUri, 0, schemeBytes.length);
		System.arraycopy(uriBytes, 0, fullUri, schemeBytes.length, uriBytes.length);
		Uri uri = Uri.parse(new String(fullUri, Charset.forName("UTF-8")));
		return new NfcUriRecord(record, uri);
	}

	public static boolean isUri(NdefRecord record) {
		try {
			parse(record);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(this.record, flags);
		dest.writeParcelable(this.mUri, flags);
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcUriRecord#getContextType()
	 */
	@Override
	public String getContextType() {
		return "org.ambientdynamix.contextplugins.nfc.uri_record";
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcUriRecord#getStringRepresentation(java.lang.String)
	 */
	@Override
	public String getStringRepresentation(String format) {
		if (format.equalsIgnoreCase("text/plain"))
		{
			return mUri.toString();
		}
		else if(format.equalsIgnoreCase("RDF/XML"))
		{
			String result="<rdf:RDF\n" +
					"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
					"xmlns:z.0=\"http://dynamix.org/semmodel/org.ambientdynamix.contextplugins.nfc/0.1/\"\n" +
					"xmlns:z.1=\"http://dynamix.org/semmodel/0.1/\" > \n";
			result=result+"<rdf:Description rdf:about=\"http://dynamix.org/semmodel/org.ambientdynamix.contextplugins.nfc/0.1/"+mUri.toString()+"\">\n";
			result=result+"<rdf:type>http://dynamix.org/semmodel/0.1/org.ambientdynamix.contextplugins.nfc.uri_record</rdf:type>\n";
			result=result+"<z.0:hasTagURI>"+mUri.toString()+"</z.0:hasTagURI>\n";
			result=result+"</rdf:Description>\n </rdf:RDF>";
			return result;
		}
		else
		{
			return "";
		}
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcUriRecord#getImplementingClassname()
	 */
	@Override
	public String getImplementingClassname() {
		return this.getClass().getName();
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcUriRecord#getStringRepresentationFormats()
	 */
	@Override
	public Set<String> getStringRepresentationFormats() {
		Set<String> formats = new HashSet<String>();
		formats.add("text/plain");
		return formats;
	};

	public IBinder asBinder() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcUriRecord#describeContents()
	 */
	public int describeContents() {
		return 0;
	}
}
