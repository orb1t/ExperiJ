package me.jezza.experij.asm;

import static me.jezza.experij.lib.Strings.format;

import java.util.ArrayList;
import java.util.List;

import me.jezza.experij.ExperiJ;
import me.jezza.experij.descriptor.Descriptor;
import me.jezza.experij.descriptor.Param;
import me.jezza.experij.interfaces.Control;
import me.jezza.experij.interfaces.Experiment;
import me.jezza.experij.lib.Equality;
import me.jezza.experij.repackage.org.objectweb.asm.ClassVisitor;
import me.jezza.experij.repackage.org.objectweb.asm.MethodVisitor;
import me.jezza.experij.repackage.org.objectweb.asm.Opcodes;

/**
 * @author Jezza
 */
public final class ClassExperiment implements Opcodes {
	/**
	 * The default method access flags to be used when generating methods.
	 */
	public static final int DEFAULT_METHOD_ACCESS = ACC_PRIVATE | ACC_SYNTHETIC | ACC_FINAL;

	/**
	 * The default method access flags to be used when generating static methods.
	 */
	public static final int DEFAULT_STATIC_METHOD_ACCESS = DEFAULT_METHOD_ACCESS | ACC_STATIC;

	/**
	 * The signature format that all the equality methods follow.
	 * Temporarily, they all go through {@link Equality}.
	 */
	private static final String EQUAL_SIGNATURE_FORMAT = "({}{})Z";

	/**
	 * The class name that all the experiment data was found in.
	 */
	private final String className;

	/**
	 * Name of the experiment.
	 */
	private final String name;

	/**
	 * If the experiments are static, and should be treated as such.
	 */
	private final boolean staticAccess;

	/**
	 * The entry point's method name.
	 */
	private final String entryPoint;

	/**
	 * The name of the renamed control method.
	 */
	private String controlMethod;

	/**
	 * The names of all the experiments to run along side the control.
	 */
	private final List<String> methodNames;

	/**
	 * The descriptor of all the methods.
	 * NOTE: If any of the methods have a differing descriptor an exception WILL be thrown.
	 */
	private Descriptor desc = null;

	protected ClassExperiment(String className, String experimentName, boolean staticAccess) {
		this.className = className;
		this.name = experimentName;
		this.staticAccess = staticAccess;
		this.entryPoint = format(ExperiJ.ENTRY_POINT_FORMAT, experimentName);
		methodNames = new ArrayList<>(3);
		System.out.println("Registering: " + experimentName);
	}

	public String name() {
		return name;
	}

	public String className() {
		return className;
	}

	public String controlMethod() {
		return controlMethod;
	}

	public String names(int index) {
		return methodNames.get(index);
	}

	/**
	 * @return - The descriptor that all experiment methods and the control method should match.
	 */
	public Descriptor desc() {
		return desc;
	}

	/**
	 * @return - The bytecode value that has to be used when loading objects from the experiments.
	 */
	public int loadCode() {
		int loadCode = desc.returnPart().loadCode;
		if (loadCode < 0)
			throw new IllegalStateException("Invalid load code call.");
		return loadCode;
	}

	/**
	 * @return - The bytecode value that has to be used when storing objects from the experiments.
	 */
	public int storeCode() {
		int storeCode = desc.returnPart().storeCode;
		if (storeCode < 0)
			throw new IllegalStateException("Invalid store code call.");
		return storeCode;

	}

	/**
	 * @return - The bytecode value that has to be used when returning objects from the experiments.
	 */
	public int returnCode() {
		return desc.returnPart().returnCode;
	}

	/**
	 * @return - The bytecode value that has to be used when invoking the experiment methods.
	 */
	public int invokeCode() {
		return staticAccess ? INVOKESTATIC : INVOKEVIRTUAL;
	}

	/**
	 * @return - The access flags that have to be used when generating methods. (Accounts for static methods)
	 */
	public int methodAccess() {
		return staticAccess ? DEFAULT_STATIC_METHOD_ACCESS : DEFAULT_METHOD_ACCESS;
	}

	/**
	 * @return - The size of all the experiments, not including the control.
	 */
	public int size() {
		return methodNames.size();
	}

	/**
	 * @return - The first free memory index. Accounts for parameters, and 'this'.
	 */
	public int firstIndex() {
		return staticAccess ? desc.parameterCount() : desc.parameterCount() + 1;
	}

	/**
	 * Loads all the method's parameters onto the stack, ready to be used by another method.
	 * Typically used before firing the control and the experiments.
	 *
	 * @param mv - The {@link MethodVisitor} that should be used to write the instructions.
	 * @return - Chaining
	 */
	public ClassExperiment loadAllParameters(MethodVisitor mv) {
		int count = desc.parameterCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				Param param = desc.parameter(i);
				int loadCode = param.loadCode;
				if (loadCode < 0)
					throw new IllegalStateException("Illegal load code call");
				mv.visitVarInsn(loadCode, param.index);
			}
		}
		return this;
	}

	/**
	 * Inserts the bytecode instructions to dynamically equate the first two objects on the stack.
	 * The method it uses to determine which method to call is based on the return type.
	 *
	 * @param mv - The {@link MethodVisitor} that should be used to write the instructions.
	 * @return - Chaining
	 */
	public ClassExperiment invokeEquals(MethodVisitor mv) {
		Param param = desc.returnPart();
		String target = param.data.length() > 1 ? "Ljava/lang/Object;" : param.data;
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Equality.INTERNAL_NAME, "equals", format(EQUAL_SIGNATURE_FORMAT, target, target), false);
		return this;
	}

	/**
	 * Registers a method as apart of this experiment set.
	 *
	 * @param methodName - The name of the method that the annotation was found on.
	 * @param desc       - The descriptor of the method, used to confirm that the methods have the same signature.
	 * @param control    - If this method is the control of the experiment set.
	 */
	public ClassExperiment register(String methodName, Descriptor desc, boolean control, boolean staticAccess) {
		if (this.staticAccess != staticAccess)
			throw new IllegalStateException("Multiple method signatures across experiment: " + name + ". All methods of an experiment have to have the same method signatures for now. (Including static bound methods). This might change in the future, if it does, this exception will be removed.");
		int hash = desc.hashCode();
		if (this.desc == null) {
			this.desc = desc;
		} else if (hash != this.desc.hashCode()) {
			throw new IllegalStateException("Multiple method signatures across experiment: " + name + ". All methods of an experiment have to have the same method signatures for now. This might change in the future, if it does, this exception will be removed.");
		}
		if (control) {
			if (this.controlMethod != null)
				throw new IllegalStateException("There are multiple control methods declared for the experiment: " + name);
			controlMethod = methodName;
		} else {
			if (methodNames.contains(methodName) || !methodNames.add(methodName))
				// I have no idea how this could even happen...
				throw new IllegalStateException("There are multiple experiment methods with the same name: " + methodName);
		}
		return this;
	}

	/**
	 * Used to confirm that the experiment was built correctly.
	 * Will throw exceptions if the data within this class is incorrect.
	 */
	public void validate() {
		if (controlMethod == null || desc == null)
			throw new IllegalStateException(format("No @{} was provided for: {}.", Control.class.getSimpleName(), name));
		if (methodNames.isEmpty())
			throw new IllegalStateException(format("No @{} was provided for: {}.", Experiment.class.getSimpleName(), name));
	}

	public ExperimentVisitor createEntryPoint(ClassVisitor cv) {
		return createMethod(cv, methodAccess(), entryPoint);
	}

	public ExperimentVisitor createMethod(ClassVisitor cv, String name) {
		return createMethod(cv, methodAccess(), name);
	}

	public ExperimentVisitor createMethod(ClassVisitor cv, int access, String name) {
		Descriptor desc = this.desc;
		return new ExperimentVisitor(this, cv.visitMethod(access, name, desc.toString(), desc.signature(), desc.exceptions()), staticAccess);
	}

	public ClassExperiment invokeEntryPoint(MethodVisitor mv) {
		return invoke(mv, entryPoint);
	}

	public ClassExperiment invokeControl(MethodVisitor mv) {
		return invoke(mv, format(ExperiJ.RENAMED_METHOD_FORMAT, controlMethod));
	}

	public ClassExperiment invokeExperiment(MethodVisitor mv, int index) {
		return invoke(mv, format(ExperiJ.RENAMED_METHOD_FORMAT, methodNames.get(index)));
	}

	private ClassExperiment invoke(MethodVisitor mv, String methodName) {
		mv.visitMethodInsn(invokeCode(), className, methodName, desc.toString(), false);
		return this;
	}
}
