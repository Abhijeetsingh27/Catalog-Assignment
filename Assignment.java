import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;

/**
 * A record to hold a single share, which is a point (x, y) on the polynomial.
 * BigInteger is used to handle arbitrarily large numbers as required by the constraints.
 */
record Share(BigInteger x, BigInteger y) {}

public class ShamirSolver {

    /**
     * Main method to run the solution for both test cases.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Test Case 1 from the problem description
        String testCase1 = """
        {
            "keys": { "n": 4, "k": 3 },
            "1": { "base": "10", "value": "4" },
            "2": { "base": "2", "value": "111" },
            "3": { "base": "10", "value": "12" },
            "6": { "base": "4", "value": "213" }
        }
        """;

        // Test Case 2 from the problem description
        String testCase2 = """
        {
            "keys": { "n": 10, "k": 7 },
            "1": { "base": "6", "value": "13444211440455345511" },
            "2": { "base": "15", "value": "aed7015a346d63" },
            "3": { "base": "15", "value": "6aeeb69631c227c" },
            "4": { "base": "16", "value": "e1b5e05623d881f" },
            "5": { "base": "8", "value": "316034514573652620673" },
            "6": { "base": "3", "value": "2122212201122002221120200210011020220200" },
            "7": { "base": "3", "value": "20120221122211000100210021102001201112121" },
            "8": { "base": "6", "value": "20220554335330240002224253" },
            "9": { "base": "12", "value": "45153788322a1255483" },
            "10": { "base": "7", "value": "1101613130313526312514143" }
        }
        """;
        
        System.out.println("Solving for Test Case 1...");
        BigInteger secret1 = findSecret(testCase1);
        System.out.println("Secret (c) for Test Case 1: " + secret1);
        
        System.out.println("\nSolving for Test Case 2...");
        BigInteger secret2 = findSecret(testCase2);
        System.out.println("Secret (c) for Test Case 2: " + secret2);
    }

    /**
     * Parses the JSON input, extracts the necessary points (shares), and computes the secret.
     *
     * @param jsonInput The string containing the test case in JSON format.
     * @return The calculated secret 'c' as a BigInteger.
     */
    public static BigInteger findSecret(String jsonInput) {
        // 1. Parse the JSON input
        JSONObject data = new JSONObject(jsonInput);
        int k = data.getJSONObject("keys").getInt("k");

        // 2. Decode the points (shares) from the JSON data
        List<Share> shares = new ArrayList<>();
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            // The points are identified by numeric keys
            if (key.matches("\\d+")) { 
                JSONObject pointData = data.getJSONObject(key);
                BigInteger x = new BigInteger(key);
                int base = Integer.parseInt(pointData.getString("base"));
                String valueStr = pointData.getString("value");
                BigInteger y = new BigInteger(valueStr, base); // Decode y from its base
                shares.add(new Share(x, y));
            }
        }
        
        // As per the problem, n >= k, so we can take the first k shares to solve.
        List<Share> sharesForReconstruction = shares.subList(0, k);

        // 3. Find the secret (c) using Lagrange Interpolation
        return reconstructSecret(sharesForReconstruction);
    }

    /**
     * Calculates the constant term 'c' (the secret) of the polynomial f(x)
     * using Lagrange Interpolation. The secret is the value of the polynomial at x=0.
     * f(0) = Σ [y_i * L_i(0)] for i = 0 to k-1
     * where L_i(0) is the Lagrange basis polynomial evaluated at 0.
     * L_i(0) = Π [(-x_j) / (x_i - x_j)] for j = 0 to k-1, j != i
     *
     * @param shares A list of k (x, y) points required to define the polynomial.
     * @return The secret, f(0), as a BigInteger.
     */
    public static BigInteger reconstructSecret(List<Share> shares) {
        BigInteger secret = BigInteger.ZERO;
        int k = shares.size();

        for (int i = 0; i < k; i++) {
            Share currentShare = shares.get(i);
            BigInteger xi = currentShare.x();
            BigInteger yi = currentShare.y();

            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) {
                    continue; // Skip the case where j equals i
                }
                BigInteger xj = shares.get(j).x();
                
                // Numerator term: -xj
                numerator = numerator.multiply(xj.negate());
                
                // Denominator term: (xi - xj)
                denominator = denominator.multiply(xi.subtract(xj));
            }

            // Calculate the full term for this share: yi * (numerator / denominator)
            // The division must be exact since all coefficients are integers.
            BigInteger term = yi.multiply(numerator).divide(denominator);
            
            // Add the term to the total sum for the secret
            secret = secret.add(term);
        }
        return secret;
    }
}