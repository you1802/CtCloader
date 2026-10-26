package you.fileserver.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ADMINのみアクセス可能なメソッドまたはクラスにつけるカスタムアノテーション
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole(T(you.fileserver.config.Constants).AUTH_ADMIN)")
public @interface IsAdmin {
}
