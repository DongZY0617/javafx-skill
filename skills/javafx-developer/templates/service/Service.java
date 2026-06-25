package {{packageName}}.service;

/**
 * Service 层模板。
 * 封装业务逻辑，与 Controller/ViewModel 解耦。
 * Controller/ViewModel 通过此层访问数据源。
 */
public class Service<T> {

    // TODO: 注入 Repository 或数据访问层

    /**
     * 根据ID查询单条记录
     */
    public T findById(Long id) {
        // TODO: 实现查询逻辑
        throw new UnsupportedOperationException("尚未实现");
    }

    /**
     * 保存记录（新增或更新）
     */
    public T save(T entity) {
        // TODO: 实现保存逻辑
        throw new UnsupportedOperationException("尚未实现");
    }

    /**
     * 根据ID删除记录
     */
    public void deleteById(Long id) {
        // TODO: 实现删除逻辑
        throw new UnsupportedOperationException("尚未实现");
    }
}
