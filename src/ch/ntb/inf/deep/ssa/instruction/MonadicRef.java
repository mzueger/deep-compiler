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

package ch.ntb.inf.deep.ssa.instruction;

import ch.ntb.inf.deep.cgPPC.RegAllocator;
import ch.ntb.inf.deep.classItems.ClassMember;
import ch.ntb.inf.deep.classItems.Item;
import ch.ntb.inf.deep.ssa.SSAValue;

public class MonadicRef extends Monadic {
	public Item item;
	
	public MonadicRef(int opCode, Item item, SSAValue operand, int bca) {
		super(opCode, operand, bca);
		this.item = item;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(result.n + ": ");
		sb.append("MonadicRef["+ scMnemonics[ssaOpcode]+"] {"+ operands[0].n + "}");
		if (item != null) {
			sb.append(" <" + item.name);
			if (item.type != null) sb.append("(" + item.type.name + ")");
			sb.append(">");
		}
		if(item instanceof ClassMember) {sb.append("Owner: " + ((ClassMember)item).owner.name.toString());}
		sb.append(" (" + result.typeName() + ")");
		if (result.index != -1) sb.append(", index=" + result.index);
		if (result.join != null) {
			sb.append(", join=[" + result.index + "(");
			SSAValue join = RegAllocator.joins[result.index];
			int i = 0;
			while (join != null && join != result.join) {
				i++;
				join = join.next;
			}
			sb.append(i + ")]");
		} else {
			sb.append(", end=" + result.end);
			if (result.reg != -1) {
				if (result.nonVol) sb.append(", nonVol"); else sb.append(", vol");
			}
			if (result.regLong != -1) sb.append(", regLong=" + result.regLong);
			if (result.reg != -1) sb.append(", reg=" + result.reg);
			if (result.regGPR1 != -1) sb.append(", regAux1=" + result.regGPR1);
		}
		sb.append(", bca=" + bca);
		return sb.toString();
	}
	

}
