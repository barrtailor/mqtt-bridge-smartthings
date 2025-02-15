/**
 *  Tasmota Switch Device Handler
 *
 *  Authors
 *	 - sandeep gupta
 *
 *	Version 1.0 - 11/17/2019
 *
 *  Copyright 2019
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
 
metadata {

    definition (name: "Tasmota Switch", namespace: "gupta/mqtt", author: "Sandeep Gupta") {
		capability "Actuator"
        capability "Switch"
		capability "Refresh"
		
		command "processMQTT"
		
		attribute "update", "string"		
		attribute "device_details", "string"
        attribute "details", "string"
        attribute "wifi", "string"
        attribute "rssiLevel", "number"
		attribute "healthStatus", "string"
	}

	simulator { 
		status "on": "switch:on"
		status "off": "switch:off"
    }

    tiles {	
		multiAttributeTile(name:"main", type: "device.switch", width: 6, height: 4, canChangeIcon: 'true', canChangeBackground : 'true' ){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}		
				
			tileAttribute("device.device_details", key: "SECONDARY_CONTROL") {
				attributeState("default", action: "refresh", label: '${currentValue}', icon:"https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/refresh.png")				
                attributeState("refresh", label: 'Updating data from server...')
			}
		}
		
		standardTile("switchon", "device.switch",  width: 2, height: 2, decoration: "flat") {
			state("on", label: 'ON', action: "switch.on", icon: "st.Home.home30")
			state("off", label: 'ON', action: "switch.on", icon: "st.Home.home30")
		}
		
		
		
		standardTile("switchoff", "device.switch",  width: 2, height: 2, decoration: "flat") {
			state("off", label: 'OFF', action: "switch.off", icon: "st.Home.home30")
			state("on", label: 'OFF', action: "switch.off", icon: "st.Home.home30")
		}

		valueTile("wifi", "device.wifi", width: 1, height: 1, decoration: "flat") {
			state ("default", label: '${currentValue}', backgroundColor: "#e86d13", icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/blank1x1-orange.png")
		}

		standardTile("rssiLevel", "device.rssiLevel", width: 1, height: 1, decoration: "flat") {
			state ("1",  icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/wifi0.png")
			state ("2",  icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/wifi1.png")
			state ("3",  icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/wifi2.png")
			state ("4",  icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/wifi3.png")
		}
		
		standardTile("healthStatus", "device.healthStatus", width: 2, height: 1, decoration: "flat") {
			state "default",  icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/online1x2.png"
			state "online",  icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/online1x2.png"
			state "offline", icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/offline1x2.png"
		}
		
		valueTile("details", "device.details", width: 6, height: 2, decoration: "flat") {
			state "default", label: '${currentValue}', icon: "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/blank1x3-green.png", backgroundColor: "#90d2a7"
		}

		main "main"
		details(["main", "switchon","healthStatus", "switchoff","wifi", "rssiLevel","details" ])
    }
}


def parse(String description) {
	log.debug "Parsing message  is ${description}"
	def pair = description.split(":")
	switch(pair[0].trim()){
		case 'switch':	
				(pair[1].trim() == "on") ? on() : off();
				break;
		default:
				break;
	}
}

def installed() {
    configure()
	refresh()
}

def refresh(){
	sendEvent(name : "update", value : 'refresh', isStateChange: 'true', descriptionText : 'Refreshing from Server...');	
	log.debug "Sent 'refresh' command for device: ${device}"
}

def ping(){
	return ((device.currentValue('healthStatus')?: "offline") == "online")
}

def configure() {
    log.trace "Executing 'configure'"
    sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)
    markDeviceOnline()
    initialize()
}

def markDeviceOnline() {
	state.pingState = 'online';
    setDeviceHealth("online")
}

def markDeviceOffline() {
	state.pingState = 'offline';
    setDeviceHealth("offline")
}

private setDeviceHealth(String healthState) {
    log.debug("healthStatus: ${device.currentValue('healthStatus')}; DeviceWatch-DeviceStatus: ${device.currentValue('DeviceWatch-DeviceStatus')}")
    // ensure healthState is valid
    List validHealthStates = ["online", "offline"]
    healthState = validHealthStates.contains(healthState) ? healthState : device.currentValue("healthStatus")
    // set the healthState
    sendEvent(name: "DeviceWatch-DeviceStatus", value: healthState)
    sendEvent(name: "healthStatus", value: healthState)
}

def processMQTT(attribute, value){
	//log.debug "Processing ${attribute} Event:  ${value} from MQTT for device: ${device}"	
	switch (attribute) {
		case 'update':
			updateTiles(value);
			break;
		default:
			break;
	}
}
	
def updateTiles(Object val ){
		//log.debug "Msg ${val}"
		if (['online','offline'].contains(val.toLowerCase())){
			log.debug "Received Health Check LWT event ${val}"
			(val.toLowerCase() == 'online') ? markDeviceOnline() : markDeviceOffline()		
			return;			
		}
		
		state.updates = (state.updates == null)  ? "" : state.updates +  val + "\n";
		def value = parseJson(val);	
		
		state.update1 = value?.Status ? true : state.update1 ?: false
		state.update2 = value?.StatusFWR ? true : state.update2 ?: false
		state.update3 = value?.StatusNET ? true : state.update3 ?: false
		state.update4 = value?.StatusSTS ? true : state.update4 ?: false
		
		state.topic = (value?.Status?.Topic) ?: state.topic
		state.friendlyName = (value?.Status?.FriendlyName) ?: state.friendlyName
		state.firmware = (value?.StatusFWR?.Version) ?: state.firmware 
		state.macAddress = ( value?.StatusNET?.Mac) ?: state.macAddress
		state.ipAddress = (value?.StatusNET?.IPAddress) ?: state.ipAddress
		if (value?.StatusSTS?.Time) state.currentTimestamp = Date.parse("yyyy-MM-dd'T'HH:mm:ss",value?.StatusSTS?.Time).format("EEE MMM dd, yyyy 'at' hh:mm:ss a")
		state.ssid1 = (value?.StatusSTS?.Wifi?.SSId) ?: state.ssid1
		state.upTime = (value?.StatusSTS?.Uptime) ?: state.upTime
		state.RSSI = (value?.StatusSTS?.Wifi?.RSSI) ?: state.RSSI
		state.rssiLevel = (value?.StatusSTS?.Wifi?.RSSI) ? (0..10).contains(state.RSSI) ? 1 
									   : (11..45).contains(state.RSSI)? 2
									   : (46..80).contains(state.RSSI)? 3
									   : (81..100).contains(state.RSSI) ? 4 : 5
									   : state.rssiLevel
									   
		//log.debug "Are updates ready ${state.update1}, ${state.update2}, ${state.update3}, ${state.update4}"
		//log.debug "Time is  ${state.currentTimestamp}"	
		if (state.update1 && state.update2 && state.update3 && state.update4){
			state.update1 = state.update2 = state.update3 = state.update4 = false;	
			runIn(3,fireEvents)
		}	
}

def fireEvents(){	
	sendEvent(name: 'device_details', value: state.topic + ", running for: " + state.upTime + 
	"\nIP: " + state.ipAddress + " [ " + state.ssid1+": "+state.RSSI + "% ]", displayed: 'false')				
	sendEvent(name: 'details', value: state.topic + "\n" + state.friendlyName + "\n" + state.ipAddress + " [ " +state.macAddress + " ]\n"  + 
	 state.firmware + " - Up Time: " + state.upTime + "\nLast Updated: " + state.currentTimestamp +"\n\n" ,  displayed: 'false')
	//sendEvent(name: 'healthStatus', value: (state.pingState?:'online') , displayed: 'false')					
	(state.RSSI < 100) ? sendEvent(name: 'wifi', value: state.RSSI +"%\nRSSI\n\n", displayed: 'false')
					   : sendEvent(name: 'wifi', value: state.RSSI +"%\nRSSI\n\n\n",  displayed: 'false')
	sendEvent(name: 'rssiLevel', value: state.rssiLevel, displayed: 'false')
	log.debug "Processed Status updates for device: [${device}]\n  ${state.updates}"
	state.updates = "";
	state.update1 = state.update2 = state.update3 = state.update4 = false;	
}

def on(){
	if (device.currentValue("switch") == "on") return;
	_on();
}

def off(){
	if (device.currentValue("switch") == "off") return;
	_off();
}

def _on() {
    sendEvent(name: "switch", value: "on")
	log.debug "Sent 'on' command for device: ${device}"
}

def _off() {
    sendEvent(name: "switch", value: "off")
	log.debug "Sent 'off' command for device: ${device}"
}

