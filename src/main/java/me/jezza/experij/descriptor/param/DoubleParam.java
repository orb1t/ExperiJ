package me.jezza.experij.descriptor.param;

import me.jezza.experij.descriptor.Param;
import me.jezza.experij.descriptor.Param;
import me.jezza.experij.repackage.org.objectweb.asm.Opcodes;

/**
 * @author Jezza
 */
public final class DoubleParam extends Param {
	public DoubleParam(int index, int arrayCount) {
		super(index, arrayCount, "D");
	}

	@Override
	public int loadCode() {
		return Opcodes.DLOAD;
	}

	@Override
	public int returnCode() {
		return Opcodes.DRETURN;
	}

	@Override
	public int storeCode() {
		return Opcodes.DSTORE;
	}
}