package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.annotation.Annotation;

/**
 * @param descriptor never null
 * @param returnTypeRequired true if the method return type is required
 * @param acceptParameter true if the method accepts parameter
 * @param annotationClass null if not annotated
 */
public record GizmoMemberInfo(GizmoMemberDescriptor descriptor, boolean returnTypeRequired,
        boolean acceptParameter, Class<? extends Annotation> annotationClass) {

}
