/*
 * Copyright 2011 - 2013 NTB University of Applied Sciences in Technology
 * Buchs, Switzerland, http://www.ntb.ch/inf
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package ch.ntb.inf.deep.config;

import ch.ntb.inf.deep.classItems.Item;
import ch.ntb.inf.deep.strings.HString;

public class RegisterInit extends Item {
	
	public Register reg;
	public int initValue;
	
	public RegisterInit(Register register, int initValue){
		this.name = HString.getRegisteredHString(register.name + "_init");
		this.reg = register;
		this.initValue = initValue;
//		if (Configuration.dbg) vrb.println("[CONF] adding init register " + reg.name + " = 0x" + Integer.toHexString(initValue));
	}
	
	@Override
	public String toString(){
		return reg.name.toString() + String.format(" = 0x%08X", initValue);
	}

}
