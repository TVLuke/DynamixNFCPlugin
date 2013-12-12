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

import java.util.ArrayList;
import java.util.List;

import org.ambientdynamix.api.application.IContextInfo;
import org.ambientdynamix.api.contextplugin.AutoContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.NfcListener;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.os.Parcelable;
import android.util.Log;

/**
 * Experimental context plug-in for detecting NFC tag events.
 * 
 * @author Darren Carlson
 * 
 */
public class NfcPluginRuntime extends AutoContextPluginRuntime implements NfcListener {
	/*
	 * Links:
	 * http://www.androidadb.com/source/zxing-read-only/core/src/com/google/zxing/client/result/optional/NDEFRecord
	 * .java.html
	 * http://developer.android.com/resources/samples/NFCDemo/src/com/example/android/nfc/record/TextRecord.html
	 * http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/nfc/ForegroundDispatch.html
	 * http://stackoverflow.com/questions/5126982/android-application-with-nfc
	 * http://stackoverflow.com/questions/5949893/android-nfc-foreground-dispatch-problem There are two ways to get NFC
	 * events; Intent registration and enableForegroundDispatch
	 * http://stackoverflow.com/questions/7655863/android-read-actual-data-from-rfid-tag
	 * http://code.google.com/p/android/issues/detail?id=15639
	 */
	private final String TAG = this.getClass().getSimpleName();

	/** Parse an NdefMessage */
	public static List<ParsedNdefRecord> parse(NdefMessage message) {
		return getRecords(message.getRecords());
	}

	public static List<ParsedNdefRecord> getRecords(NdefRecord[] records) {
		List<ParsedNdefRecord> elements = new ArrayList<ParsedNdefRecord>();
		for (NdefRecord record : records) {
			try {
				if (NfcUriRecord.isUri(record)) {
					elements.add(NfcUriRecord.parse(record));
				}
				if (NfcTextRecord.isText(record)) {
					elements.add(NfcTextRecord.parse(record));
				}
				if (NfcSmartPoster.isPoster(record)) {
					elements.add(NfcSmartPoster.parse(record));
				}
			} catch (Exception e) {
			}
		}
		return elements;
	}

	public void setPowerScheme(PowerScheme scheme) {
		// No support needed
	}

	@Override
	public void start() {
		/*
		 * The Dynamix ContextManager will dispatch NfcEvents when it's enabled, so there's nothing to do.
		 */
		Log.i(TAG, this + " is Started!");
	}

	public void stop() {
		/*
		 * The Dynamix ContextManager will not dispatch NfcEvents when it's disabled, so there's nothing to do.
		 */
		Log.i(TAG, this + " is Stopped!");
	}

	public void destroy() {
		// Remove our Nfc listener
		getPluginFacade().removeNfcListener(this.getSessionId(), this);
		Log.i(TAG, this + " is Destroyed!");
	}

	@Override
	public void init(PowerScheme scheme, ContextPluginSettings settings) throws Exception {
		// Store the incoming settings
		this.setPowerScheme(scheme);
		// Make sure this device has an NfcManager
		Context c = getPluginFacade().getSecuredContext(getSessionId());
		NfcManager mgr = (NfcManager) c.getSystemService(Context.NFC_SERVICE);
		if (mgr == null)
			throw new RuntimeException("No NFC Hardware Detected!");
		// Register for Nfc Events using Dynamix
		getPluginFacade().addNfcListener(this.getSessionId(), this);
		Log.i(TAG, "Initialized for: " + this);
	}

	@Override
	public void updateSettings(ContextPluginSettings settings) {
		// Not supported
	}

	@Override
	public void doManualContextScan() {
		// Not supported
	}

	@Override
	public void onNfcEvent(Intent i) {
		Tag tag = i.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		if (tag != null) {
			byte[] uidBytes = i.getByteArrayExtra(NfcAdapter.EXTRA_ID);
			Log.d(TAG, "Received NFC Tag with UID: " + NfcTag.byteArrayToHexString(uidBytes));
			sendContextEvent(new SecuredContextInfo(new NfcTag(tag), PrivacyRiskLevel.MEDIUM), 60000);
		}
		Log.d(TAG, "Checking for NDEF messages... ");
		Parcelable[] rawMsgs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] msgs;
		if (rawMsgs != null) {
			// Known tag type
			msgs = new NdefMessage[rawMsgs.length];
			for (int r = 0; r < rawMsgs.length; r++) {
				msgs[r] = (NdefMessage) rawMsgs[r];
			}
		} else {
			// Unknown tag type
			byte[] empty = new byte[] {};
			NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
			NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
			msgs = new NdefMessage[] { msg };
		}
		Log.i(TAG, "Extracted NdefMessage count: " + msgs.length);
		for (NdefMessage msg : msgs) {
			Iterable<ParsedNdefRecord> records = getRecords(msg.getRecords());
			for (ParsedNdefRecord record : records) {
				IContextInfo event = null;
				if (record.getClass().equals(NfcUriRecord.class)) {
					NfcUriRecord r = (NfcUriRecord) record;
					Log.i(TAG, "Detected NfcUriRecord: " + r.getUri());
					event = r;
				}
				if (record.getClass().equals(NfcTextRecord.class)) {
					NfcTextRecord r = (NfcTextRecord) record;
					Log.i(TAG, "Detected NfcTextRecord: " + r.getText());
					event = r;
				}
				if (record.getClass().equals(NfcSmartPoster.class)) {
					NfcSmartPoster r = (NfcSmartPoster) record;
					Log.i(TAG, "Detected NfcSmartPoster: " + r.getTitle());
					event = r;
				}
				if (event != null) {
					sendContextEvent(new SecuredContextInfo(event, PrivacyRiskLevel.MEDIUM), 60000);
				} else
					Log.d(TAG, "No NDEF messages found!");
			}
		}
	}
}