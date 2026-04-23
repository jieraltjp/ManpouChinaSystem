import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter @Setter @Builder
@Slf4j
public class Test {
    private String name;
    private int value;
}
