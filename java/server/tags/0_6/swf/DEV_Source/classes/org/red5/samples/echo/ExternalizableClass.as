package org.red5.samples.echo {

import flash.utils.IDataInput;
import flash.utils.IDataOutput;
import flash.utils.IExternalizable;

import mx.controls.Alert;

[RemoteClass(alias="org.red5.server.webapp.echo.ExternalizableClass")]
public class ExternalizableClass implements IExternalizable {
	
	public var valid: Number;
	public var failed: Number;
	
	public function ExternalizableClass() {
		valid = 0;
		failed = 0;
	}
	
	public function writeExternal(output: IDataOutput): void {
		output.writeBoolean(true);
		output.writeBoolean(false);
		output.writeByte(0);
		output.writeByte(-1);
		output.writeByte(1);
		output.writeByte(127);
		output.writeByte(-127);
		// TODO: output.writeBytes
		output.writeDouble(1.0);
		output.writeFloat(2.0);
		output.writeInt(0);
		output.writeInt(-1);
		output.writeInt(1);
		output.writeMultiByte("\xe4\xf6\xfc\xc4\xd6\xdc\xdf", "iso-8859-1");
		output.writeMultiByte("\xe4\xf6\xfc\xc4\xd6\xdc\xdf", "utf-8");
		var ob: Array = new Array();
		ob.push(1);
		ob.push("one");
		ob.push(1.0);
		output.writeObject(ob);
		output.writeShort(0);
		output.writeShort(-1);
		output.writeShort(1);
		output.writeUnsignedInt(0);
		output.writeUnsignedInt(1);
		output.writeUTF("Hello world!");
		output.writeUTFBytes("Hello world!");
	}
    
	private function checkEqual(a: Object, b: Object): void {
		if (a == b) {
			valid += 1;
		} else {
			failed += 1;
		}
	}
	
	public function readExternal(input: IDataInput): void {
		checkEqual(input.readBoolean(), true);
		checkEqual(input.readBoolean(), false);
		checkEqual(input.readByte(), 0);
		checkEqual(input.readByte(), -1);
		checkEqual(input.readByte(), 1);
		checkEqual(input.readByte(), 127);
		checkEqual(input.readByte(), -127);
		// TODO: input.readBytes
		checkEqual(input.readDouble(), 1.0);
		checkEqual(input.readFloat(), 2.0);
		checkEqual(input.readInt(), 0);
		checkEqual(input.readInt(), -1);
		checkEqual(input.readInt(), 1);
		checkEqual(input.readMultiByte(7, "iso-8859-1"), "\xe4\xf6\xfc\xc4\xd6\xdc\xdf");
		checkEqual(input.readMultiByte(14, "utf-8"), "\xe4\xf6\xfc\xc4\xd6\xdc\xdf");
		var ob: Object = input.readObject();
		if (ob is Array) {
			if (ob.length == 3) {
				if (ob[0] == 1 && ob[1] == "one" && ob[2] == 1.0)
					valid += 1;
				else
					failed += 1;
			} else {
				failed += 1;
			}
		} else {
			failed += 1;
		}
		checkEqual(input.readShort(), 0);
		checkEqual(input.readShort(), -1);
		checkEqual(input.readShort(), 1);
		checkEqual(input.readUnsignedInt(), 0);
		checkEqual(input.readUnsignedInt(), 1);
		checkEqual(input.readUTF(), "Hello world!");
		checkEqual(input.readUTFBytes(12), "Hello world!");
	}
}

}