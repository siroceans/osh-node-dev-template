package com.georobotix.impl.sensor.PlanetaryPositions;

import com.sun.jdi.connect.Connector;
import org.ejml.simple.SimpleMatrix;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

public class Planet {
    private String name;

    // Orbital elements at the j2000 Epoch
    private final double mu = 1;
    private final double s2tu = 1 / (5.0226757 * Math.pow(10, 6));
    private double a; //AU
    private double e; // eccentricity
    private double i; // to ecliptic [deg]
    private double RAAN; // deg
    private double omega; // deg
    private double theta; // deg
    private double[] rvec = new double[3]; // Position vector in the J2000 epoch in the IJK reference frame
    private double[] vvec = new double[3]; // Velocity vector in the J2000 epoch in the IJK reference frame

    // Current values
    private double[] rCurrent = new double[3];
    private double[] vCurrent = new double[3];
    private double currentTime; // Time elapsed snce J2000 Epoch! [TU]

    public Planet(String planetName) {
        // Setting Orbital Elements for each planet. Observations made in the J2000 Epoch!
        switch (planetName.toLowerCase()) {
            case ("mercury"):
                this.a = 0.387099;
                this.e = 0.205631;
                this.i = 7.00487;
                this.RAAN = 48.33167;
                this.omega = 29.12478;
                this.theta = 174.7944;
                break;
            case ("venus"):
                this.a = 0.723332;
                this.e = 0.006773;
                this.i = 3.39471;
                this.RAAN = 76.68069;
                this.omega = 54.85229;
                this.theta = 50.44675;
                break;
            case("earth"):
                this.a = 1.000000;
                this.e = 0.01671;
                this.i = 0.00005;
                this.RAAN = -11.26064;
                this.omega = 114.20783;
                this.theta = -2.48284;
                break;
            case("mars"):
                this.a = 1.523662;
                this.e = 0.093412;
                this.i = 1.85061;
                this.RAAN = 49.57854;
                this.omega = 286.4623;
                this.theta = 19.41248;
                break;
            case("jupiter"):
                this.a = 5.203363;
                this.e = 0.048393;
                this.i = 1.3053;
                this.RAAN = 100.55615;
                this.omega = -85.8023;
                this.theta = 19.55053;
                break;
            case("saturn"):
                this.a = 9.537070;
                this.e = 0.054151;
                this.i = 2.48446;
                this.RAAN = 113.71504;
                this.omega = -21.2831;
                this.theta = -42.4876;
                break;
            case("uranus"):
                this.a = 19.19126;
                this.e = 0.047168;
                this.i = 0.76986;
                this.RAAN = 74.22988;
                this.omega = 96.73436;
                this.theta = 142.2679;
                break;
            case("neptune"):
                this.a = 30.06896;
                this.e = 0.008586;
                this.i = 1.76917;
                this.RAAN = 131.72169;
                this.omega = -86.75034;
                this.theta = 259.9087;
                break;
            case("pluto"):
                this.a = 39.48169;
                this.e = 0.248808;
                this.i = 17.14175;
                this.RAAN = 110.30347;
                this.omega = 113.76329;
                this.theta = 14.86205;
                break;
        }

        // Setting planet name and position at the J2000 epoch
        this.name = planetName.toUpperCase();
        setOriginalPosition();
    }

    private void setCurrentTime() {
        Instant j2000 = ZonedDateTime.of(
                2000, 1, 1, 11, 58, 55, 0, ZoneOffset.UTC)
                .toInstant();
        long currentTimeUnix = System.currentTimeMillis(); // Current time relative to Unix Epoch
        long j2000Unix =  j2000.toEpochMilli(); // j2000 in ms relative to Unix epoch

        this.currentTime = (double) (currentTimeUnix - j2000Unix) / 1000 * s2tu;
    }

    private void setOriginalPosition() {
        // Sets position and velocity at the J2000 Epoch
        double p = a * (1 - Math.pow(e, 2));
        double r = p / (1 + e * Math.cos(Math.toRadians(theta)));

        // Calculating r and v in the pqw reference frame
        SimpleMatrix r_pqw = new SimpleMatrix(1,3);
        SimpleMatrix v_pqw = new SimpleMatrix(1,3);
        r_pqw.set(0,0,
                r * Math.cos(Math.toRadians(theta)));

        r_pqw.set(0,1,
                r * Math.sin(Math.toRadians(theta)));

        v_pqw.set(0, 0,
                - Math.sin(Math.toRadians(theta)));

        v_pqw.set(0, 1,
                e + Math.cos(Math.toRadians(theta)));

        v_pqw = v_pqw.scale(Math.sqrt(mu/p));

        // Rotating the components to the IJK frame
        double R11 = Math.cos(Math.toRadians(RAAN)) * Math.cos(Math.toRadians(omega)) -
                Math.sin(Math.toRadians(RAAN)) * Math.sin(Math.toRadians(omega)) * Math.cos(Math.toRadians(i));
        double R12 = - Math.cos(Math.toRadians(RAAN)) * Math.sin(Math.toRadians(omega)) -
                Math.sin(Math.toRadians(RAAN)) * Math.cos(Math.toRadians(omega)) * Math.cos(Math.toRadians(i));
        double R13 = Math.sin(Math.toRadians(RAAN)) * Math.sin(Math.toRadians(i));
        double R21 = Math.sin(Math.toRadians(RAAN)) * Math.cos(Math.toRadians(omega)) +
                Math.cos(Math.toRadians(RAAN)) * Math.sin(Math.toRadians(omega)) * Math.cos(Math.toRadians(i));
        double R22 = - Math.sin(Math.toRadians(RAAN)) * Math.sin(Math.toRadians(omega)) +
                Math.cos(Math.toRadians(RAAN)) * Math.cos(Math.toRadians(omega)) * Math.cos(Math.toRadians(i));
        double R23 = - Math.cos(Math.toRadians(RAAN)) * Math.sin(Math.toRadians(i));
        double R31 = Math.sin(Math.toRadians(omega)) * Math.sin(Math.toRadians(i));
        double R32 = Math.cos(Math.toRadians(omega)) * Math.sin(Math.toRadians(i));
        double R33 = Math.cos(Math.toRadians(i));

        SimpleMatrix R = new SimpleMatrix(new double[][]{
                {R11, R12, R13},
                {R21, R22, R23},
                {R31, R32, R33}
        });

        SimpleMatrix r_ijk = R.mult(r_pqw.transpose()).transpose();
        SimpleMatrix v_ijk = R.mult(v_pqw.transpose()).transpose();

        // Setting Planet Values
        rvec[0] = r_ijk.get(0, 0);
        rvec[1] = r_ijk.get(0, 1);
        rvec[2] = r_ijk.get(0, 2);

        vvec[0] = v_ijk.get(0, 0);
        vvec[1] = v_ijk.get(0, 1);
        vvec[2] = v_ijk.get(0, 2);
    }

    private void calculateCurrent() {
        final double tol = Math.pow(10, -7);
        final int maxno = 10;

        setCurrentTime(); // Initially calculate the current time wrt the j2000 epocha
        double[] r_orig = Arrays.copyOf(rvec, rvec.length);
        double[] v_orig = Arrays.copyOf(vvec, vvec.length);
        SimpleMatrix r_scale = new SimpleMatrix(1, 3, true, rvec);
        SimpleMatrix v_scale = new SimpleMatrix(1, 3, true, vvec);

         double rmag = magnitude(r_orig);
         double vmag = magnitude(v_orig);
         double[] h = cross(r_orig, v_orig);
         double hmag = magnitude(h);
         SimpleMatrix e_vec = r_scale.scale((Math.pow(vmag,2) - (mu/rmag))).minus(
                 v_scale.scale(dot(r_orig, v_orig)));
         e_vec = e_vec.scale(1/mu);
         double[] evec = {e_vec.get(0,0), e_vec.get(0,1), e_vec.get(0,2)};
         double emag = magnitude(evec);
         double a = Math.pow(hmag, 2) / (mu * (1 - Math.pow(emag, 2)));
         double n = Math.sqrt(1 / Math.pow(a, 3));

         // Iterating!
         double xn = Math.sqrt(mu) * currentTime/a;
         int i = 0;
         boolean iterate = true;
         double s = 0.0;
         double c = 0.0;
         double t = 0.0;
         double z = 0.0;
         double rn = 0.0;

         while (iterate) {
             i++;
             z = Math.pow(xn, 2) / a;

             if (Math.abs(z) < tol){
                 s =  1.0 / 6.0 - z / 120.0 + Math.pow(z,2) / (5040) - Math.pow(z, 3) / (362880.0);
                 c = 1.0 / (2.0) - z / (24.0) + Math.pow(z, 2) / (720.0) - Math.pow(z, 3) / (40320.0);
             }
             else if (z > 0) {
                 s = (Math.sqrt(z) - Math.sin(Math.sqrt(z))) / Math.sqrt(Math.pow(z, 3));
                 c = (1.0 - Math.cos(Math.sqrt(z))) / z;
             }
             else {
                 s = (Math.sinh(Math.sqrt(-z)) - Math.sqrt(-z)) / Math.sqrt(Math.pow(-z, 3));
                 c = (1.0 - Math.cosh(Math.sqrt(-z))) / z;
             }

             t = (1.0 / Math.sqrt(mu)) * (Math.pow(xn, 3) * s + dot(r_orig, v_orig) / Math.sqrt(mu) *
                     Math.pow(xn, 2) * c + rmag * xn * (1.0 - z * s));
             rn = Math.pow(xn, 2) * c + dot(r_orig, v_orig) / Math.sqrt(mu) * xn * (1.0 - z * s) + rmag *
                     (1.0 - z * c);
             double dtdx = rn / Math.sqrt(mu);
             double xn1 = xn + (currentTime - t) / dtdx;

             if (Math.abs(currentTime - t) < tol) {
                 iterate = false;
             }
             else {
                 xn = xn1;
             }

             if (i > maxno) iterate = false;
         }

         /*
        System.out.println("xn");
        System.out.println(xn);
        System.out.println("rmag");
        System.out.println(rmag);
        System.out.println("c");
        System.out.println(c);
        System.out.println("s");
        System.out.println(s);
        System.out.println("z");
        System.out.println(z);
          */

         double f = 1 - Math.pow(xn, 2) / rmag * c;
         double g = t - Math.pow(xn, 3) / Math.sqrt(mu) * s;

         double fdot = (Math.sqrt(mu) * xn) / (rmag * rn) * (z * s - 1);
         double gdot = 1 - Math.pow(xn, 2) / rn * c;

         SimpleMatrix rCurrentMat = r_scale.scale(f).plus(v_scale.scale(g));
         SimpleMatrix vCurrentMat = r_scale.scale(fdot).plus(v_scale.scale(gdot));

         // Assigning values
        rCurrent[0] = rCurrentMat.get(0, 0);
        rCurrent[1] = rCurrentMat.get(0, 1);
        rCurrent[2] = rCurrentMat.get(0, 2);

        vCurrent[0] = vCurrentMat.get(0, 0);
        vCurrent[1] = vCurrentMat.get(0, 1);
        vCurrent[2] = vCurrentMat.get(0, 2);
    }

    private static double magnitude(double[] vector) {
        return Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2) + Math.pow(vector[2], 2));
    }

    private static double[] cross(double[] a, double[] b) {
        double[] result = new double[3];

        result[0] = a[1] * b[2] - a[2] * b[1];
        result[1] = a[2] * b[0] - a[0] * b[2];
        result[2] = a[0] * b[1] - a[1] * b[0];

        return result;
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static double factorial(int n) {
        if (n < 0)  return 0;

        double result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public double[] getCurrentPosition() {
        calculateCurrent();
        return rCurrent;
    }

    public double[] getCurrentVelocity() {
        calculateCurrent();
        return vCurrent;
    }

    public String getPlanetName() {
        return this.name.toUpperCase();
    }

}
