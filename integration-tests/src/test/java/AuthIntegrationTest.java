import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

public class AuthIntegrationTest {
    /* Test Setup */
    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004";
    }

    /* Tests */
    // Positive Test (Happy Test)
    @Test
    public void shouldReturnOKWithValidToken() {
        // 1. Arrange
        String loginPayload = """
            {
                "email": "testuser@test.com",
                "password": "password123"
            }
            """;

        Response response = RestAssured.given()
            .contentType("application/JSON")
            .body(loginPayload)
            .when()
            .post("/auth/login") // 2. Act
            .then() // 3. Assert
            .statusCode(200) // expected status code
            .body("token", notNullValue()) // token field in response shouldn't be null
            .extract()
            .response();

        System.out.println("Generated Token: " + response.jsonPath().getString("token"));
    }

    // Negative Test (Un-Happy Test)
    @Test
    public void shouldReturnUnauthorizedOnInvalidLogin() {
        // 1. Arrange
        String loginPayload = """
            {
                "email": "invalid_user@test.com",
                "password": "wrong_password"
            }
            """;

        RestAssured.given()
            .contentType("application/JSON")
            .body(loginPayload)
            .when()
            .post("/auth/login") // 2. Act
            .then() // 3. Assert
            .statusCode(401);
    }
}
