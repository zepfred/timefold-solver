package ai.timefold.solver.core.impl.solver.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

@SupportedAnnotationTypes("ai.timefold.solver.core.api.domain.entity.PlanningEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class PlanningEntityAnnotationProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : annotatedElements) {
                try {
                    writeBuilderFile(element);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    private void writeBuilderFile(Element element) throws IOException {
        String packageName = null;
        String className = ((TypeElement) element).getQualifiedName().toString();
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }
        String builderClassName = className + "Enhanced";
        String builderSimpleClassName = builderClassName
                .substring(lastDot + 1);

        Element enclosingElement = element.getEnclosingElement();
        if (enclosingElement != null && enclosingElement.getKind().isClass()) {
            while (enclosingElement != null && enclosingElement.getKind().isClass()) {
                enclosingElement = enclosingElement.getEnclosingElement();
            }
            lastDot = ((PackageElement) enclosingElement).getQualifiedName().toString().length();
            if (lastDot > 0) {
                packageName = className.substring(0, lastDot);
            }
            builderClassName = packageName + "." + element.getSimpleName() + "Enhanced";
            builderSimpleClassName = builderClassName.substring(lastDot + 1);
        }

        JavaFileObject builderFile = processingEnv.getFiler()
                .createSourceFile(builderClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }

            out.print("public class ");
            out.print(builderSimpleClassName);
            out.println(" {");
            out.println();

            out.println("}");
        }
    }
}
