package io.robe.resources;

import com.google.inject.Inject;
import com.yammer.dropwizard.auth.Auth;
import com.yammer.dropwizard.hibernate.UnitOfWork;
import io.robe.auth.Credentials;
import io.robe.hibernate.dao.QuartzJobDao;
import io.robe.hibernate.entity.QuartzJob;
import io.robe.quartz.Lighter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by sinanselimoglu on 19/02/14.
 */
@Path("quartzJob")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QuartzJobResource {
    @Inject
    QuartzJobDao quartzJobDao;

    @GET
    @UnitOfWork
    public List<QuartzJob> getAll(@Auth Credentials credentials) {
        List<QuartzJob> list = quartzJobDao.findAll(QuartzJob.class);
        return list;
    }

    @PUT
    @Path("/update")
    @UnitOfWork
    public QuartzJob setCron(QuartzJob quartzJob) {
        quartzJobDao.update(quartzJob);
        return quartzJob;
    }

    @PUT
    @Path("/fire")
    @UnitOfWork
    public String fireJob(QuartzJob quartzJob) {
        Lighter lighter=new Lighter();
        String cron = lighter.fire(quartzJob);
        return cron;
    }

}
