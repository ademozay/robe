package io.robe.hibernate.dao;

import com.google.inject.Inject;
import io.robe.hibernate.entity.QuartzJob;
import org.hibernate.SessionFactory;

/**
 * Created by sinanselimoglu on 19/02/14.
 */
public class QuartzJobDao extends BaseDao<QuartzJob>{
    /**
     * Constructor with session factory injection by guice
     *
     * @param sessionFactory injected session factory
     */
    @Inject
    public QuartzJobDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }
}
