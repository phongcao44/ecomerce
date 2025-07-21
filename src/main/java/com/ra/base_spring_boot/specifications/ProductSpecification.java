
import com.ra.base_spring_boot.model.Product;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

public class ProductSpecification {

    public static Specification<Product> hasColorId(Long colorId) {
        return (root, query, cb) -> {
            if (colorId == null) return null;

            Join<Object, Object> variantJoin = root.join("variants", JoinType.LEFT);
            return cb.equal(variantJoin.get("color").get("id"), colorId);
        };
    }

    public static Specification<Product> hasSizeId(Long sizeId) {
        return (root, query, cb) -> {
            if (sizeId == null) return null;

            Join<Object, Object> variantJoin = root.join("variants", JoinType.LEFT);
            return cb.equal(variantJoin.get("size").get("id"), sizeId);
        };
    }
}
