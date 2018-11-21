MIDIWatcher {
	classvar <process, <>frequency = 0.5;
	classvar sources, destinations;
	classvar <listeners;

	*initClass {
		MIDIClient.init;
		sources = IdentityDictionary();
		destinations = IdentityDictionary();
		listeners = IdentityDictionary();
	}

	*start {
		if (process.notNil) {
			process.stop();
		};

		process = SkipJack({ MIDIWatcher.update }, { MIDIWatcher.frequency }, name:MIDIWatcher);
	}

	*stop {
		process.stop();
	}

	*update {
		var oldSources, oldDestinations;
		oldSources = sources ?? { () };
		oldDestinations = destinations ?? { () };

		MIDIClient.list;

		sources = MIDIClient.sources.collectAs({ |e| e.asSymbol -> e }, IdentityDictionary);
		destinations = MIDIClient.sources.collectAs({ |e| e.asSymbol -> e}, IdentityDictionary);

		oldSources.keys.difference(sources.keys).do {
			|removed|
			this.onChange(\sourceRemoved, oldSources[removed]);
		};
		oldDestinations.keys.difference(destinations.keys).do {
			|removed|
			this.onChange(\destinationRemoved, oldDestinations[removed]);
		};

		sources.keys.difference(oldSources.keys).do {
			|added|
			this.onChange(\sourceAdded, sources[added])
		};
		destinations.keys.difference(oldDestinations.keys).do {
			|added|
			this.onChange(\destinationAdded, destinations[added])
		};

		this.changed();
	}

	*deviceSignal {
		|device="*", name="*"|
		var key = (device ++ "_" ++ name).asSymbol;

		^listeners.atFail(key, {
			var sig = UpdateBroadcaster(key);
			listeners[key] = sig;
			sig;
		})
	}

	*onChange {
		|type, endpoint|
		var deviceListener, nameListener, deviceNameListener, starStarSymbol;

		nameListener= ("*_" ++ endpoint.name).asSymbol;
		deviceListener  = (endpoint.device ++ "_*").asSymbol;
		deviceNameListener = (endpoint.device ++ "_" ++ endpoint.name).asSymbol;
		starStarSymbol = '*_*';

		[deviceListener, nameListener, deviceNameListener, starStarSymbol].do {
			|symbol|

			listeners[symbol].do {
				|listener|
				listener.changed(type, endpoint);
			}
		}
	}
}

+MIDIEndPoint {
	asSymbol {
		^(device ++ "_" ++ name ++ "_" ++ uid).asSymbol;
	}
}