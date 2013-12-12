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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.ambientdynamix.api.application.IContextInfo;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Adapted from http://developer.android.com/resources/samples/NFCDemo/src/com/example/android/nfc/record/SmartPoster.html
 */
class NfcSmartPoster implements IContextInfo, ParsedNdefRecord, INfcSmartPoster {
	private static final byte[] ACTION_RECORD_TYPE = new byte[] { 'a', 'c', 't' };
	private static final byte[] TYPE_TYPE = new byte[] { 't' };
	public static Parcelable.Creator<NfcSmartPoster> CREATOR = new Parcelable.Creator<NfcSmartPoster>() {
		public NfcSmartPoster createFromParcel(Parcel in) {
			return new NfcSmartPoster(in);
		}

		public NfcSmartPoster[] newArray(int size) {
			return new NfcSmartPoster[size];
		}
	};

	private enum RecommendedAction {
		UNKNOWN((byte) -1), DO_ACTION((byte) 0), SAVE_FOR_LATER((byte) 1), OPEN_FOR_EDITING((byte) 2);
		private static final Map<Byte, RecommendedAction> LOOKUP;
		static {
			LOOKUP = new HashMap<Byte, RecommendedAction>();
			for (RecommendedAction action : RecommendedAction.values()) {
				LOOKUP.put(action.getByte(), action);
			}
		}
		private final byte mAction;

		private RecommendedAction(byte val) {
			this.mAction = val;
		}

		private byte getByte() {
			return mAction;
		}
	}

	/**
	 * Returns the first element of {@code elements} which is an instance of {@code type}, or {@code null} if no such
	 * element exists.
	 */
	private static <T> T getFirstIfExists(Iterable<?> elements, Class<T> type) {
		for (Iterator<?> it = elements.iterator(); it.hasNext();) {
			Object tmp = it.next();
			if (tmp.getClass().equals(type.getClass()))
				return (T) tmp;
		}
		return null;
	}

	private static NdefRecord getByType(byte[] type, NdefRecord[] records) {
		for (NdefRecord record : records) {
			if (Arrays.equals(type, record.getType())) {
				return record;
			}
		}
		return null;
	}

	private static RecommendedAction parseRecommendedAction(NdefRecord[] records) {
		NdefRecord record = getByType(ACTION_RECORD_TYPE, records);
		if (record == null) {
			return RecommendedAction.UNKNOWN;
		}
		byte action = record.getPayload()[0];
		if (RecommendedAction.LOOKUP.containsKey(action)) {
			return RecommendedAction.LOOKUP.get(action);
		}
		return RecommendedAction.UNKNOWN;
	}

	private static String parseType(NdefRecord[] records) {
		NdefRecord type = getByType(TYPE_TYPE, records);
		if (type == null) {
			return null;
		}
		return new String(type.getPayload(), Charset.forName("UTF-8"));
	}

	/**
	 * NFC Forum Smart Poster Record Type Definition section 3.2.1. "The Title record for the service (there can be many
	 * of these in different languages, but a language MUST NOT be repeated). This record is optional."
	 */
	private final NfcTextRecord mTitleRecord;
	/**
	 * NFC Forum Smart Poster Record Type Definition section 3.2.1. "The URI record. This is the core of the Smart
	 * Poster, and all other records are just metadata about this record. There MUST be one URI record and there MUST
	 * NOT be more than one."
	 */
	private final NfcUriRecord mUriRecord;
	/**
	 * NFC Forum Smart Poster Record Type Definition section 3.2.1. "The Action record. This record describes how the
	 * service should be treated. For example, the action may indicate that the device should save the URI as a bookmark
	 * or open a browser. The Action record is optional. If it does not exist, the device may decide what to do with the
	 * service. If the action record exists, it should be treated as a strong suggestion; the UI designer may ignore it,
	 * but doing so will induce a different user experience from device to device."
	 */
	private final RecommendedAction mAction;
	private final byte recommendedActionByte;
	/**
	 * NFC Forum Smart Poster Record Type Definition section 3.2.1. "The Type record. If the URI references an external
	 * entity (e.g., via a URL), the Type record may be used to declare the MIME type of the entity. This can be used to
	 * tell the mobile device what kind of an object it can expect before it opens the connection. The Type record is
	 * optional."
	 */
	private final String mType;

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcSmartPoster#getNfcUriRecord()
	 */
	public INfcUriRecord getNfcUriRecord() {
		return mUriRecord;
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcSmartPoster#getTitle()
	 */
	public INfcTextRecord getTitle() {
		return mTitleRecord;
	}

	public static NfcSmartPoster parse(NdefRecord record) throws Exception {
		if (record.getTnf() != NdefRecord.TNF_WELL_KNOWN)
			throw new Exception();
		if (record.getType() != NdefRecord.RTD_SMART_POSTER)
			throw new Exception();
		try {
			NdefMessage subRecords = new NdefMessage(record.getPayload());
			return parse(subRecords.getRecords());
		} catch (FormatException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static NfcSmartPoster parse(NdefRecord[] recordsRaw) {
		try {
			Iterable<ParsedNdefRecord> records = NfcPluginRuntime.getRecords(recordsRaw);
			NfcUriRecord uri = null;
			for (ParsedNdefRecord record : records) {
				if (record.getClass().equals(NfcUriRecord.class))
					uri = (NfcUriRecord) record;
			}
			NfcTextRecord title = getFirstIfExists(records, NfcTextRecord.class);
			RecommendedAction action = parseRecommendedAction(recordsRaw);
			String type = parseType(recordsRaw);
			return new NfcSmartPoster(uri, title, action, type);
		} catch (NoSuchElementException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static boolean isPoster(NdefRecord record) {
		try {
			parse(record);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private NfcSmartPoster(final Parcel in) {
		this.mTitleRecord = in.readParcelable(this.getClass().getClassLoader());
		this.mUriRecord = in.readParcelable(this.getClass().getClassLoader());
		this.mType = in.readString();
		this.recommendedActionByte = in.readByte();
		this.mAction = RecommendedAction.LOOKUP.get(recommendedActionByte);
	}

	private NfcSmartPoster(NfcUriRecord uri, NfcTextRecord title, RecommendedAction action, String type) {
		mUriRecord = uri;
		mTitleRecord = title;
		mAction = action;
		recommendedActionByte = action.getByte();
		mType = type;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(this.mTitleRecord, flags);
		dest.writeParcelable(this.mUriRecord, flags);
		dest.writeString(this.mType);
		dest.writeByte(recommendedActionByte);
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcSmartPoster#getContextType()
	 */
	@Override
	public String getContextType() {
		return "org.ambientdynamix.contextplugins.nfc.smart_poster";
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcSmartPoster#getImplementingClassname()
	 */
	@Override
	public String getImplementingClassname() {
		return this.getClass().getName();
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcSmartPoster#getStringRepresentation(java.lang.String)
	 */
	@Override
	public String getStringRepresentation(String format) {
		return "";
	}

	/* (non-Javadoc)
	 * @see org.ambientdynamix.contextplugins.nfc.INfcSmartPoster#getStringRepresentationFormats()
	 */
	@Override
	public Set<String> getStringRepresentationFormats() {
		return new HashSet<String>();
	}
}
