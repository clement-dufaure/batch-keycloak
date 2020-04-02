package re.dufau.keycloak;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import re.dufau.keycloak.tasks.CloneClients;

@Configuration
@EnableBatchProcessing
public class CloneClientJob extends DefaultBatchConfigurer {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public CloneClients taskCloneClient;

    @Override
    public void setDataSource(DataSource dataSource) {
        // initialize will use a Map based JobRepository (instead of database)
    }

    @Bean
    public Job cloneClientsJob(Step step1) {
        return jobBuilderFactory.get("cloneClients").start(step1).build();
    }

    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1").tasklet(taskCloneClient).build();
    }

}