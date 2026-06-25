package {{packageName}}.model;

import java.time.LocalDateTime;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * 通用可观察模型，提供标准的 JavaFX 属性支持。
 * <p>
 * 名称使用 {@link StringProperty}，创建时间使用 {@link ObjectProperty}，
 * 从而支持自动的 UI 绑定与变更跟踪。
 * </p>
 */
public class ObservableModel {

    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final ObjectProperty<LocalDateTime> createdAt =
            new SimpleObjectProperty<>(this, "createdAt");

    /**
     * 创建一个使用默认名称与当前时间戳的新模型。
     */
    public ObservableModel() {
        this.createdAt.set(LocalDateTime.now());
    }

    /**
     * 创建一个使用给定名称与当前时间戳的新模型。
     *
     * @param name 初始名称
     */
    public ObservableModel(String name) {
        this();
        setName(name);
    }

    /**
     * @return 名称属性
     */
    public final StringProperty nameProperty() {
        return name;
    }

    /**
     * @return 当前名称值
     */
    public final String getName() {
        return name.get();
    }

    /**
     * 设置名称值。
     *
     * @param name 新名称
     */
    public final void setName(String name) {
        this.name.set(name);
    }

    /**
     * @return 创建时间属性
     */
    public final ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }

    /**
     * @return 创建时间值
     */
    public final LocalDateTime getCreatedAt() {
        return createdAt.get();
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 新的创建时间
     */
    public final void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }

    @Override
    public String toString() {
        return "ObservableModel{name=" + getName() + ", createdAt=" + getCreatedAt() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObservableModel that = (ObservableModel) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getCreatedAt(), that.getCreatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getCreatedAt());
    }
}
