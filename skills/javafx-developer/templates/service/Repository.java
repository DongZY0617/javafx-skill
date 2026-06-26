package {{packageName}}.service;

import java.util.List;
import java.util.Optional;

/**
 * 仓储层通用接口模板。
 * <p>
 * 定义实体的标准 CRUD 数据访问契约，与具体持久化技术（数据库、文件、内存等）解耦。
 * Service 层通过此接口访问数据源，便于替换底层存储实现或进行单元测试 Mock。
 * </p>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface Repository<T, ID> {

    /**
     * 根据主键查询单个实体。
     *
     * @param id 主键值，不应为 {@code null}
     * @return 包含实体的 {@link Optional}；若不存在对应记录则返回 {@link Optional#empty()}
     */
    Optional<T> findById(ID id);

    /**
     * 查询全部实体。
     *
     * @return 实体列表；若无数据返回空列表（不应返回 {@code null}）
     */
    List<T> findAll();

    /**
     * 保存实体（新增或更新）。
     * <p>
     * 若实体携带的主键在持久层中已存在，则执行更新；否则执行新增。
     * </p>
     *
     * @param entity 待保存的实体，不应为 {@code null}
     * @return 保存后的实体（可能包含生成的主键或版本号）
     */
    T save(T entity);

    /**
     * 根据主键删除实体。
     * <p>
     * 若主键对应的记录不存在，本方法应静默返回而不抛出异常。
     * </p>
     *
     * @param id 主键值，不应为 {@code null}
     */
    void deleteById(ID id);
}
