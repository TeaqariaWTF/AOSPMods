package sh.siava.pixelxpert.annotationprocessor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import sh.siava.pixelxpert.annotations.*;

@SupportedAnnotationTypes("sh.siava.pixelxpert.annotations.BaseModPack")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class AnnotationProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if(annotations.isEmpty())
			return true;

		Set<? extends Element> modPackAnnotations = roundEnv.getElementsAnnotatedWith(BaseModPack.class);

		try {
			JavaFileObject javaClass = processingEnv.getFiler().createSourceFile("sh.siava.pixelxpert.xposed.ModPacks");
			try (Writer writer = javaClass.openWriter()) {
				writer.write("""
						package sh.siava.pixelxpert.xposed;
						
						import java.util.*;
						
						import sh.siava.pixelxpert.annotations.*;
						import sh.siava.pixelxpert.xposed.modpacks.dialer.*;
						public class ModPacks {
						\tpublic static List<ModPackData> getModPacks() {
						\t\tList<ModPackData> result = new ArrayList<>();
						""");

				for(Element modPackAnnotation : modPackAnnotations){
					String targetPackage = modPackAnnotation.getAnnotation(BaseModPack.class).targetPackage();
					Set<? extends Element> targetModPacks = roundEnv.getElementsAnnotatedWith((TypeElement) modPackAnnotation);
					processModPacks(targetPackage, targetModPacks, writer);
				}

				writer.write("""
						\t\treturn result;
						\t}
						}
						""");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	private void processModPacks(String targetPackage, Set<? extends Element> targetModPacks, Writer writer) throws IOException {
		for (Element targetModPack : targetModPacks) {

			String qualifiedName = ((TypeElement)targetModPack).getQualifiedName().toString();

			boolean mainProcessPack = true, childProcessPack = false;
			ChildProcessModPack childProcessData = targetModPack.getAnnotation(ChildProcessModPack.class);
			String childProcessName = "";
			if(childProcessData != null)
			{
				childProcessPack = true;
				childProcessName = childProcessData.processNameContains();
				mainProcessPack = targetModPack.getAnnotation(MainProcessModPack.class) != null;
			}

			writer.write(String.format("\t\tresult.add(new ModPackData(%s.class, \"%s\", %s, %s, \"%s\"));\n", qualifiedName, targetPackage, mainProcessPack, childProcessPack, childProcessName));

			new ModPackData(targetModPack.getClass(), targetPackage, mainProcessPack, childProcessPack, "");
		}
	}
}