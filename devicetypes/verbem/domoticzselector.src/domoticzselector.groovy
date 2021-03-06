/**
 *  Domoticz Selector SubType Switch.
 *
 *  SmartDevice type for domoticz selector switches.
 *  
 *
 *  Copyright (c) 2016 Martin Verbeek
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *  Revision History
 *  ----------------
 *  2017-04-14 3.12 Multistate support for DZ selector
 *  2017-01-25 3.09 Put in check for switch name in generateevent
 *	2017-01-18 3.08 get always an lowercase value for switch on/off in generateevent
 */

metadata {
    definition (name:"domoticzSelector", namespace:"verbem", author:"Martin Verbeek") {
        capability "Actuator"
        capability "Sensor"
        capability "Switch"
        capability "Switch Level"
        capability "Refresh"
        capability "Polling"
        capability "Signal Strength"
		capability "Health Check"
        
        attribute "selector", "enum", [true, false]
        
        // custom commands
        command "parse"     // (String "<attribute>:<value>[,<attribute>:<value>]")
       	command "setLevel"
        command "generateEvent"
        command "stateNext"
        command "statePrev"
    }

    tiles(scale:2) {
    	multiAttributeTile(name:"richDomoticzSelector", type:"generic",  width:6, height:4) {
        	tileAttribute("device.selectorState", key: "PRIMARY_CONTROL") {
                attributeState "level", label:'${currentValue}', icon:"st.Electronics.electronics13", backgroundColor: "#00a0dc", defaultState: true
				attributeState "Off", label:'${name}', icon:"st.Electronics.electronics13", backgroundColor: "#ffffff"
				attributeState "Alarm", label:'${name}', icon:"st.Electronics.electronics13", backgroundColor: "#e86d13"
				attributeState "Away", label:'${name}', icon:"st.Electronics.electronics13", backgroundColor: "#cccccc"
				attributeState "Home", label:'${name}', icon:"st.Electronics.electronics13", backgroundColor: "#00a0dc"
				attributeState "Error", label:"Install Error", backgroundColor: "#bc2323"
            }
            
            tileAttribute("device.level", key: "SECONDARY_CONTROL") {
            	attributeState "level", label:'Selector level ${currentValue}', defaultState: true
            }
		}
     
		standardTile("tileNext", "device.tileNext", inactiveLabel: false, width: 3, height: 2, decoration:"flat") {
			state "default", label:'Next State', action: "stateNext"
		}
     
		standardTile("tilePrev", "device.tilePrev", inactiveLabel: false, width: 3, height: 2, decoration:"flat") {
			state "default", label:'Prev State', action: "statePrev"
		}

		standardTile("rssi", "device.rssi", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "rssi", label:'Signal ${currentValue}', unit:"", icon:"https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/devicetypes/verbem/domoticzsensor.src/network-signal.png"
		}
        
        standardTile("debug", "device.motion", inactiveLabel: false, decoration: "flat", width:2, height:2) {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main(["richDomoticzSelector"])
        
        details(["richDomoticzSelector", "tilePrev", "tileNext", "rssi", "debug"])
    }
}

def parse(String message) {
    TRACE("parse(${message})")

    Map msg = stringToMap(message)
    if (msg?.size() == 0) {
        log.error "Invalid message: ${message}"
        return null
    }

    if (msg.containsKey("switch")) {
        def value = msg.switch.toInteger()
        switch (value) {
        case 0: off(); break
        case 1: on(); break
        }
    }

    STATE()
    return null
}

// switch.poll() command handler
def poll() {

    if (parent) {
        TRACE("poll() ${device.deviceNetworkId}")
        parent.domoticz_poll(getIDXAddress())
    }
}

// switch.poll() command handler
def refresh() {

    if (parent) {
        parent.domoticz_poll(getIDXAddress())
    }
}

def statePrev() {
	setLevel(device.currentValue("level")-10)
}


def stateNext() {
	setLevel(device.currentValue("level")+10)
}

def on() {

    if (parent) {
        parent.domoticz_on(getIDXAddress())
    }
}

def off() {

    if (parent) {
        parent.domoticz_off(getIDXAddress())
    }
}

def setLevel(level) {
    TRACE("setLevel Level " + level)
    state.setLevel = level
    if (parent) {
        parent.domoticz_setlevel(getIDXAddress(), level)
    }
}

private def TRACE(message) {
    log.debug message
}

private def STATE() {
    log.debug "Selector is ${device.currentValue("selectorState")}"
    log.debug "deviceNetworkId: ${device.deviceNetworkId}"
}

// gets the IDX address of the device
private getIDXAddress() {
	
    def idx = getDataValue("idx")
        
    if (!idx) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 3) {
            idx = parts[2]
        } else {
            log.warn "Can't figure out idx for device: ${device.id}"
        }
    }

    //log.debug "Using IDX: $idx for device: ${device.id}"
    return idx
}

/*----------------------------------------------------*/
/*			execute event can be called from the service manager!!!
/*----------------------------------------------------*/
def generateEvent (Map results) {
    results.each { name, value ->
    	def v = value
    	if (name == "switch") {
        	if (v instanceof String) {
                if (v.toUpperCase() == "OFF" ) v = "off"
                if (v.toUpperCase() == "ON") v = "on"
                }
            }
        log.info "generateEvent " + name + " " + v
        sendEvent(name:"${name}", value:"${v}")
        }
        return null
}

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {

	if (parent) {
        sendEvent(name: "DeviceWatch-Enroll", value: groovy.json.JsonOutput.toJson([protocol: "LAN", scheme:"untracked"]), displayed: false)
    }
    else {
    	log.error "You cannot use this DTH without the related SmartAPP Domoticz Server, the device needs to be a child of this App"
        sendEvent(name: "selectorState", value: "Error", descriptionText: "$device.displayName You cannot use this DTH without the related SmartAPP Domoticz Server", isStateChange: true)
    }
}