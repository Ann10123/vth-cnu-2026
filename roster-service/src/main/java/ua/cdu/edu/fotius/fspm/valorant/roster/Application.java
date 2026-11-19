package ua.cdu.edu.fotius.fspm.valorant.roster;

import io.micronaut.runtime.Micronaut;

public class Application {
    public static void main(String[] args) {
        String env = System.getenv().getOrDefault("VTH_ENVIRONMENT", "dev");
        Micronaut.build(args)
            .environments(env)
            .mainClass(Application.class)
            .start();
    }
}
