package com.guilhermedev.librarymedia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.guilhermedev.librarymedia.media.MediaRecord;
import com.guilhermedev.librarymedia.media.MediaType;
import com.guilhermedev.librarymedia.media.ProviderPage;
import com.guilhermedev.librarymedia.media.TmdbMediaProvider;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LibrarymediaApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@MockBean
	private TmdbMediaProvider tmdbMediaProvider;

	@BeforeEach
	void configureMediaProviderStub() {
		when(tmdbMediaProvider.providerId()).thenReturn("TMDB");
		when(tmdbMediaProvider.supports(any())).thenReturn(true);
		when(tmdbMediaProvider.findByExternalId("movie-1", MediaType.MOVIE))
				.thenReturn(new MediaRecord("TMDB", "movie-1", "MOVIE", "A Test Movie", 2024,
						"Drama", "Test synopsis", "https://example.invalid/cover.jpg", null));
		when(tmdbMediaProvider.findByExternalId("movie-1", MediaType.SERIES)).thenReturn(null);
		when(tmdbMediaProvider.search(anyString(), anyInt(), anyInt(),
				nullable(MediaType.class), nullable(Integer.class)))
				.thenReturn(new ProviderPage(java.util.List.of(
						new MediaRecord("TMDB", "movie-1", "MOVIE", "A Test Movie", 2024,
								"Drama", "Test synopsis", "https://example.invalid/cover.jpg", null)),
						1, 1));
		when(tmdbMediaProvider.findByExternalId("movie-2", MediaType.MOVIE))
				.thenReturn(new MediaRecord("tmdb", "movie-2", "MOVIE", "Alpha Movie", 2023,
						"Drama", null, null, null));
		when(tmdbMediaProvider.findByExternalId("movie-3", MediaType.MOVIE))
				.thenReturn(new MediaRecord("tmdb", "movie-3", "MOVIE", "Beta Movie", 2022,
						"Drama", null, null, null));
	}

	@Test
	void tmdbAttributionIsAvailableWithoutAuthentication() throws Exception {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/media/attribution", JsonNode.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().path("notice").asText().contains("TMDB"));
		assertTrue(response.getBody().path("url").asText().startsWith("https://"));
	}

	@Test
	void swaggerDocumentsTagsAndExclusiveMediaConflictExample() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);
		JsonNode listDelete = response.getBody().path("paths").path("/api/v1/lists/{listId}")
				.path("delete");
		assertTrue(response.getBody().path("tags").findValuesAsText("name").contains("Lists"));
		assertTrue(listDelete.path("summary").asText().contains("Delete a list"));
		assertTrue(listDelete.toString().contains("LIST_HAS_EXCLUSIVE_MEDIA"), listDelete.toPrettyString());
	}

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsAvailableWithoutAuthentication() {
		ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().contains("\"status\":\"UP\""));
	}

	@Test
	void registrationDuplicateLoginAndProtectedRouteContractsAreEnforced() throws Exception {
		String email = "duplicate-" + java.util.UUID.randomUUID() + "@example.com";
		ResponseEntity<JsonNode> registration = register(email);
		assertEquals(HttpStatus.CREATED, registration.getStatusCode());

		ResponseEntity<JsonNode> duplicate = register(email);
		assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
		assertEquals("EMAIL_ALREADY_REGISTERED", duplicate.getBody().path("code").asText());

		ResponseEntity<String> mismatchedConfirmation = restTemplate.postForEntity("/api/v1/auth/register",
				new HttpEntity<>("{\"name\":\"Reader\",\"email\":\"mismatch-" + email +
						"\",\"password\":\"a-safe-test-password\"," +
						"\"passwordConfirmation\":\"a-different-password\"}", jsonHeaders()), String.class);
		assertEquals(HttpStatus.BAD_REQUEST, mismatchedConfirmation.getStatusCode());
		assertTrue(mismatchedConfirmation.getBody().contains("VALIDATION_ERROR"));

		String oversizedUtf8Password = "é".repeat(40);
		ResponseEntity<String> oversizedPassword = restTemplate.postForEntity("/api/v1/auth/register",
				new HttpEntity<>("{\"name\":\"Reader\",\"email\":\"long-password-" + email +
						"\",\"password\":\"" + oversizedUtf8Password + "\",\"passwordConfirmation\":\"" +
						oversizedUtf8Password + "\"}", jsonHeaders()), String.class);
		assertEquals(HttpStatus.BAD_REQUEST, oversizedPassword.getStatusCode());
		assertTrue(oversizedPassword.getBody().contains("VALIDATION_ERROR"));

		ResponseEntity<JsonNode> successfulLogin = login(email);
		assertEquals(HttpStatus.OK, successfulLogin.getStatusCode());
		assertTrue(successfulLogin.getBody().has("accessToken"));
		assertEquals("Bearer", successfulLogin.getBody().path("tokenType").asText());
		assertTrue(successfulLogin.getBody().path("expiresIn").asLong() > 0);

		ResponseEntity<String> invalidLogin = restTemplate.postForEntity("/api/v1/auth/login",
				new HttpEntity<>("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}",
						jsonHeaders()), String.class);
		assertEquals(HttpStatus.UNAUTHORIZED, invalidLogin.getStatusCode());

		ResponseEntity<String> unauthorized = restTemplate.getForEntity("/api/v1/library", String.class);
		assertEquals(HttpStatus.UNAUTHORIZED, unauthorized.getStatusCode());
	}

	@Test
	void registrationCreatesFavoritesAndMeDoesNotExposePassword() throws Exception {
		ResponseEntity<JsonNode> registration = register();
		assertEquals(HttpStatus.CREATED, registration.getStatusCode());

		assertTrue(registration.getBody().has("id"));
		assertFalse(registration.getBody().has("accessToken"));
		String token = login(registration.getBody().path("email").asText()).getBody()
				.path("accessToken").asText();
		ResponseEntity<String> me = restTemplate.exchange("/api/v1/users/me", HttpMethod.GET,
				authenticated(token), String.class);
		assertEquals(HttpStatus.OK, me.getStatusCode());
		assertFalse(me.getBody().contains("password"));

		ResponseEntity<JsonNode> lists = restTemplate.exchange("/api/v1/lists", HttpMethod.GET,
				authenticated(token), JsonNode.class);
		assertEquals(1, lists.getBody().size());
		assertEquals("Favorites", lists.getBody().get(0).path("name").asText());
		assertTrue(lists.getBody().get(0).path("favorites").asBoolean());
	}

	@Test
	void libraryAddRequiresExplicitListAndRatingForCompletedMedia() throws Exception {
		String token = registerAndLogin();
		long listId = getFavoritesId(token);

		ResponseEntity<String> completedWithoutRating = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"TMDB\",\"externalId\":\"movie-1\",\"listIds\":[" +
						listId + "],\"consumed\":true}"), String.class);
		assertEquals(HttpStatus.BAD_REQUEST, completedWithoutRating.getStatusCode());
		assertTrue(completedWithoutRating.getBody().contains("RATING_REQUIRED"));

		ResponseEntity<String> ratingWhileUncompleted = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"TMDB\",\"externalId\":\"movie-1\",\"listIds\":[" +
						listId + "],\"consumed\":false,\"rating\":8}"), String.class);
		assertEquals(HttpStatus.BAD_REQUEST, ratingWhileUncompleted.getStatusCode());
		assertTrue(ratingWhileUncompleted.getBody().contains("RATING_REQUIRES_COMPLETED"));

		ResponseEntity<String> withoutListIds = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"TMDB\",\"externalId\":\"movie-1\",\"consumed\":false}"),
				String.class);
		assertEquals(HttpStatus.BAD_REQUEST, withoutListIds.getStatusCode());
		assertTrue(withoutListIds.getBody().contains("VALIDATION_ERROR"));
	}

	@Test
	void searchAndDetailsIndicateWhetherMediaIsInTheCurrentLibrary() throws Exception {
		String token = registerAndLogin();
		long listId = createList(token, "Movies");
		ResponseEntity<JsonNode> added = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"tmdb\",\"externalId\":\"movie-1\",\"listIds\":[" +
						listId + "],\"consumed\":false}"), JsonNode.class);
		long entryId = added.getBody().path("entryId").asLong();

		ResponseEntity<JsonNode> search = restTemplate.exchange(
				"/api/v1/media/search?q=movie&type=MOVIE", HttpMethod.GET,
				authenticated(token), JsonNode.class);
		JsonNode result = search.getBody().path("content").get(0);
		assertTrue(result.path("inLibrary").asBoolean());
		assertEquals(entryId, result.path("entryId").asLong());
		assertEquals(2024, result.path("year").asInt());

		ResponseEntity<JsonNode> details = restTemplate.exchange(
				"/api/v1/media/tmdb/movie-1?type=MOVIE", HttpMethod.GET,
				authenticated(token), JsonNode.class);
		assertTrue(details.getBody().path("inLibrary").asBoolean());
		assertEquals(entryId, details.getBody().path("entryId").asLong());
	}

	@Test
	void duplicateListNamesFavoritesProtectionAndEmptyItemsLinkAreEnforced() throws Exception {
		String token = registerAndLogin();
		long listId = createList(token, "Reading");

		ResponseEntity<String> duplicateName = restTemplate.exchange("/api/v1/lists", HttpMethod.POST,
				jsonRequest(token, "{\"name\":\"reading\"}"), String.class);
		assertEquals(HttpStatus.CONFLICT, duplicateName.getStatusCode());

		ResponseEntity<String> renameFavorites = restTemplate.exchange("/api/v1/lists/" +
						getFavoritesId(token), HttpMethod.PATCH,
				jsonRequest(token, "{\"name\":\"New name\"}"), String.class);
		assertEquals(HttpStatus.CONFLICT, renameFavorites.getStatusCode());
		assertTrue(renameFavorites.getBody().contains("FAVORITES_LIST_PROTECTED"));

		ResponseEntity<JsonNode> emptyItems = restTemplate.exchange("/api/v1/lists/" + listId +
						"/items?type=BOOK&status=DONE", HttpMethod.GET,
				authenticated(token), JsonNode.class);
		assertEquals(HttpStatus.OK, emptyItems.getStatusCode());
		assertEquals(0, emptyItems.getBody().path("totalElements").asInt());
		assertEquals("/api/v1/media/search", emptyItems.getBody().path("links").path("search").asText());
	}

	@Test
	void addingExistingLibraryEntryToAnotherListHasCreatedAndDuplicateResponses() throws Exception {
		String token = registerAndLogin();
		long firstListId = getFavoritesId(token);
		long secondListId = createList(token, "Another list");
		ResponseEntity<JsonNode> addedToLibrary = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"TMDB\",\"externalId\":\"movie-1\",\"listIds\":[" +
						firstListId + "],\"consumed\":false}"), JsonNode.class);
		long entryId = addedToLibrary.getBody().path("entryId").asLong();

		ResponseEntity<JsonNode> addedToSecondList = restTemplate.exchange("/api/v1/lists/" + secondListId +
						"/items", HttpMethod.POST, jsonRequest(token, "{\"entryId\":" + entryId + "}"),
				JsonNode.class);
		assertEquals(HttpStatus.CREATED, addedToSecondList.getStatusCode());
		assertFalse(addedToSecondList.getBody().path("alreadyInList").asBoolean());

		ResponseEntity<JsonNode> duplicate = restTemplate.exchange("/api/v1/lists/" + secondListId +
						"/items", HttpMethod.POST, jsonRequest(token, "{\"entryId\":" + entryId + "}"),
				JsonNode.class);
		assertEquals(HttpStatus.OK, duplicate.getStatusCode());
		assertTrue(duplicate.getBody().path("alreadyInList").asBoolean());

		ResponseEntity<JsonNode> filteredItems = restTemplate.exchange("/api/v1/lists/" + secondListId +
						"/items?type=MOVIE&status=WANT", HttpMethod.GET,
				authenticated(token), JsonNode.class);
		assertEquals(1, filteredItems.getBody().path("totalElements").asInt());

		String anotherUser = registerAndLogin();
		long anotherUsersListId = createList(anotherUser, "Their list");
		ResponseEntity<String> anotherUsersEntry = restTemplate.exchange("/api/v1/lists/" + secondListId +
						"/items", HttpMethod.POST, jsonRequest(anotherUser, "{\"entryId\":" + entryId + "}"),
				String.class);
		assertEquals(HttpStatus.NOT_FOUND, anotherUsersEntry.getStatusCode());
		ResponseEntity<String> foreignEntry = restTemplate.exchange("/api/v1/lists/" + anotherUsersListId +
						"/items", HttpMethod.POST, jsonRequest(anotherUser, "{\"entryId\":" + entryId + "}"),
				String.class);
		assertEquals(HttpStatus.NOT_FOUND, foreignEntry.getStatusCode());
	}

	@Test
	void privateListsAreNotVisibleToAnotherUserAndFavoritesCannotBeDeleted() throws Exception {
		String firstToken = registerAndLogin();
		ResponseEntity<JsonNode> list = restTemplate.exchange("/api/v1/lists",
				HttpMethod.POST, jsonRequest(firstToken, "{\"name\":\"Private list\"}"), JsonNode.class);
		assertEquals(HttpStatus.CREATED, list.getStatusCode());

		String secondToken = registerAndLogin();
		ResponseEntity<String> hiddenList = restTemplate.exchange(
				"/api/v1/lists/" + list.getBody().path("id").asLong(), HttpMethod.GET,
				authenticated(secondToken), String.class);
		assertEquals(HttpStatus.NOT_FOUND, hiddenList.getStatusCode());

		ResponseEntity<String> favoritesDelete = restTemplate.exchange("/api/v1/lists/" +
						getFavoritesId(firstToken), HttpMethod.DELETE, authenticated(firstToken), String.class);
		assertEquals(HttpStatus.CONFLICT, favoritesDelete.getStatusCode());
	}

	@Test
	void libraryStatusRatingAndLastListConfirmationRulesAreEnforced() throws Exception {
		String token = registerAndLogin();
		long listId = createList(token, "Only list");
		String addRequest = "{\"provider\":\"TMDB\",\"externalId\":\"movie-1\",\"listIds\":[" + listId +
				"],\"consumed\":true,\"rating\":8}";
		ResponseEntity<JsonNode> added = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, addRequest), JsonNode.class);
		assertEquals(HttpStatus.CREATED, added.getStatusCode());
		long entryId = added.getBody().path("entryId").asLong();
		assertEquals(listId, added.getBody().path("addedToLists").get(0).asLong());

		ResponseEntity<JsonNode> duplicateAdd = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, addRequest), JsonNode.class);
		assertEquals(HttpStatus.OK, duplicateAdd.getStatusCode());
		assertTrue(duplicateAdd.getBody().path("alreadyInLibrary").asBoolean());
		assertEquals(listId, duplicateAdd.getBody().path("alreadyInLists").get(0).asLong());
		assertEquals(8, duplicateAdd.getBody().path("rating").asInt());

		ResponseEntity<JsonNode> filteredLibrary = restTemplate.exchange(
				"/api/v1/library?type=MOVIE&status=DONE&listId=" + listId +
						"&q=test&sort=title,asc", HttpMethod.GET,
				authenticated(token), JsonNode.class);
		assertEquals(1, filteredLibrary.getBody().path("totalElements").asInt(),
				filteredLibrary.getBody().toString());
		assertEquals(1, filteredLibrary.getBody().path("content").get(0).path("lists").size());

		ResponseEntity<JsonNode> changedStatus = restTemplate.exchange("/api/v1/library/" + entryId + "/status",
				HttpMethod.PUT, jsonRequest(token, "{\"status\":\"WANT\"}"), JsonNode.class);
		assertEquals(8, changedStatus.getBody().path("rating").asInt());

		ResponseEntity<String> ratingBeforeDone = restTemplate.exchange("/api/v1/library/" + entryId + "/rating",
				HttpMethod.PUT, jsonRequest(token, "{\"rating\":9}"), String.class);
		assertEquals(HttpStatus.CONFLICT, ratingBeforeDone.getStatusCode());

		ResponseEntity<JsonNode> returnedToDone = restTemplate.exchange("/api/v1/library/" + entryId + "/status",
				HttpMethod.PUT, jsonRequest(token, "{\"status\":\"DONE\"}"), JsonNode.class);
		assertEquals(8, returnedToDone.getBody().path("rating").asInt());
		ResponseEntity<JsonNode> updatedRating = restTemplate.exchange("/api/v1/library/" + entryId + "/rating",
				HttpMethod.PUT, jsonRequest(token, "{\"rating\":9}"), JsonNode.class);
		assertEquals(9, updatedRating.getBody().path("rating").asInt());

		ResponseEntity<String> removeWithoutConfirmation = restTemplate.exchange(
				"/api/v1/lists/" + listId + "/items/" + entryId, HttpMethod.DELETE,
				authenticated(token), String.class);
		assertEquals(HttpStatus.CONFLICT, removeWithoutConfirmation.getStatusCode());
		assertTrue(removeWithoutConfirmation.getBody().contains("LAST_LIST_CONFIRMATION_REQUIRED"));

		ResponseEntity<String> removeConfirmed = restTemplate.exchange(
				"/api/v1/lists/" + listId + "/items/" + entryId + "?confirm=true", HttpMethod.DELETE,
				authenticated(token), String.class);
		assertEquals(HttpStatus.OK, removeConfirmed.getStatusCode());
		assertTrue(removeConfirmed.getBody().contains("\"removedFromLibrary\":true"));
	}

	@Test
	void deletingListWithExclusiveEntriesRequiresConfirmation() throws Exception {
		String token = registerAndLogin();
		long listId = createList(token, "Temporary");
		ResponseEntity<JsonNode> added = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"TMDB\",\"externalId\":\"movie-1\",\"listIds\":[" +
						listId + "],\"consumed\":false}"), JsonNode.class);
		long entryId = added.getBody().path("entryId").asLong();

		ResponseEntity<String> firstDelete = restTemplate.exchange("/api/v1/lists/" + listId,
				HttpMethod.DELETE, authenticated(token), String.class);
		assertEquals(HttpStatus.CONFLICT, firstDelete.getStatusCode());
		assertTrue(firstDelete.getBody().contains("LIST_HAS_EXCLUSIVE_MEDIA"));

		ResponseEntity<JsonNode> confirmedDelete = restTemplate.exchange("/api/v1/lists/" + listId +
						"?confirm=true", HttpMethod.DELETE,
				authenticated(token), JsonNode.class);
		assertEquals(HttpStatus.OK, confirmedDelete.getStatusCode());
		assertEquals(1, confirmedDelete.getBody().path("removedLibraryEntries").asInt());
		ResponseEntity<String> removedEntry = restTemplate.exchange("/api/v1/library/" + entryId,
				HttpMethod.GET, authenticated(token), String.class);
		assertEquals(HttpStatus.NOT_FOUND, removedEntry.getStatusCode());
	}

	@Test
	void concurrentRemovalFromTwoListsCannotOrphanLibraryEntry() throws Exception {
		String token = registerAndLogin();
		long firstListId = createList(token, "First list");
		long secondListId = createList(token, "Second list");
		ResponseEntity<JsonNode> added = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"tmdb\",\"externalId\":\"movie-1\",\"listIds\":[" +
						firstListId + "],\"consumed\":false}"), JsonNode.class);
		long entryId = added.getBody().path("entryId").asLong();
		restTemplate.exchange("/api/v1/lists/" + secondListId + "/items", HttpMethod.POST,
				jsonRequest(token, "{\"entryId\":" + entryId + "}"), JsonNode.class);

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<ResponseEntity<String>> firstRemoval = executor.submit(() -> {
				ready.countDown();
				start.await();
				return restTemplate.exchange("/api/v1/lists/" + firstListId + "/items/" + entryId,
						HttpMethod.DELETE, authenticated(token), String.class);
			});
			Future<ResponseEntity<String>> secondRemoval = executor.submit(() -> {
				ready.countDown();
				start.await();
				return restTemplate.exchange("/api/v1/lists/" + secondListId + "/items/" + entryId,
						HttpMethod.DELETE, authenticated(token), String.class);
			});

			assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();
			ResponseEntity<String> firstResponse = firstRemoval.get(15, TimeUnit.SECONDS);
			ResponseEntity<String> secondResponse = secondRemoval.get(15, TimeUnit.SECONDS);
			assertEquals(1, java.util.stream.Stream.of(firstResponse, secondResponse)
					.filter(response -> response.getStatusCode() == HttpStatus.OK).count());
			assertEquals(1, java.util.stream.Stream.of(firstResponse, secondResponse)
					.filter(response -> response.getStatusCode() == HttpStatus.CONFLICT).count());
		} finally {
			executor.shutdownNow();
		}

		ResponseEntity<JsonNode> remainingEntry = restTemplate.exchange("/api/v1/library/" + entryId,
				HttpMethod.GET, authenticated(token), JsonNode.class);
		assertEquals(HttpStatus.OK, remainingEntry.getStatusCode());
		assertEquals(1, remainingEntry.getBody().path("lists").size());
	}

	@Test
	void rankingsArePagedAndBreakRatingTiesByTitle() throws Exception {
		String token = registerAndLogin();
		long listId = getFavoritesId(token);
		for (String externalId : java.util.List.of("movie-1", "movie-2", "movie-3")) {
			ResponseEntity<JsonNode> added = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
					jsonRequest(token, "{\"provider\":\"tmdb\",\"externalId\":\"" + externalId +
							"\",\"type\":\"MOVIE\",\"listIds\":[" + listId +
							"],\"consumed\":true,\"rating\":8}"), JsonNode.class);
			assertEquals(HttpStatus.CREATED, added.getStatusCode());
		}

		ResponseEntity<JsonNode> firstPage = restTemplate.exchange("/api/v1/rankings/movies?page=0&size=2",
				HttpMethod.GET, authenticated(token), JsonNode.class);
		assertEquals(3, firstPage.getBody().path("totalElements").asInt());
		assertEquals(1, firstPage.getBody().path("content").get(0).path("rank").asInt());
		assertEquals("A Test Movie", firstPage.getBody().path("content").get(0).path("title").asText());
		assertEquals(2, firstPage.getBody().path("content").get(1).path("rank").asInt());

		ResponseEntity<JsonNode> secondPage = restTemplate.exchange("/api/v1/rankings/movies?page=1&size=2",
				HttpMethod.GET, authenticated(token), JsonNode.class);
		assertEquals(3, secondPage.getBody().path("content").get(0).path("rank").asInt());

		ResponseEntity<String> invalidCategory = restTemplate.exchange("/api/v1/rankings/music",
				HttpMethod.GET, authenticated(token), String.class);
		assertEquals(HttpStatus.BAD_REQUEST, invalidCategory.getStatusCode());
		assertTrue(invalidCategory.getBody().contains("VALIDATION_ERROR"));
	}

	@Test
	void publicShareContainsOnlyListAndMediaFieldsAndCanBeReactivated() throws Exception {
		String token = registerAndLogin();
		long listId = createList(token, "Shared books and films");
		ResponseEntity<JsonNode> added = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"TMDB\",\"externalId\":\"movie-1\",\"listIds\":[" +
						listId + "],\"consumed\":false}"), JsonNode.class);
		assertEquals(HttpStatus.CREATED, added.getStatusCode());
		ResponseEntity<JsonNode> secondAdded = restTemplate.exchange("/api/v1/library", HttpMethod.POST,
				jsonRequest(token, "{\"provider\":\"TMDB\",\"externalId\":\"movie-2\",\"listIds\":[" +
						listId + "],\"consumed\":false}"), JsonNode.class);
		assertEquals(HttpStatus.CREATED, secondAdded.getStatusCode());

		ResponseEntity<JsonNode> activated = restTemplate.exchange("/api/v1/lists/" + listId + "/share",
				HttpMethod.PUT, jsonRequest(token, "{\"active\":true}"), JsonNode.class);
		String shareToken = activated.getBody().path("token").asText();
		assertTrue(activated.getBody().path("active").asBoolean());
		assertTrue(activated.getBody().path("url").asText().contains(shareToken));

		ResponseEntity<JsonNode> publicList = restTemplate.getForEntity(
				"/api/v1/public/lists/" + shareToken + "?page=0&size=1", JsonNode.class);
		assertEquals("Shared books and films", publicList.getBody().path("name").asText());
		assertEquals(2, publicList.getBody().path("mediaCount").asInt());
		assertEquals(2, publicList.getBody().path("items").path("totalElements").asInt());
		assertEquals(1, publicList.getBody().path("items").path("content").size());
		JsonNode publicItem = publicList.getBody().path("items").path("content").get(0);
		assertTrue(publicItem.has("title"));
		assertFalse(publicItem.has("rating"));
		assertFalse(publicItem.has("status"));
		assertFalse(publicItem.has("id"));
		assertFalse(publicItem.has("provider"));
		assertFalse(publicList.getBody().has("owner"));

		restTemplate.exchange("/api/v1/lists/" + listId + "/share", HttpMethod.PUT,
				jsonRequest(token, "{\"active\":false}"), JsonNode.class);
		ResponseEntity<String> inactiveList = restTemplate.getForEntity(
				"/api/v1/public/lists/" + shareToken, String.class);
		assertEquals(HttpStatus.NOT_FOUND, inactiveList.getStatusCode());

		ResponseEntity<JsonNode> reactivated = restTemplate.exchange("/api/v1/lists/" + listId + "/share",
				HttpMethod.PUT, jsonRequest(token, "{\"active\":true}"), JsonNode.class);
		assertEquals(shareToken, reactivated.getBody().path("token").asText());
	}

	private ResponseEntity<JsonNode> register() {
		String email = "reader-" + java.util.UUID.randomUUID() + "@example.com";
		return register(email);
	}

	private ResponseEntity<JsonNode> register(String email) {
		return restTemplate.postForEntity("/api/v1/auth/register",
				new HttpEntity<>("{\"name\":\"Reader\",\"email\":\"" + email +
						"\",\"password\":\"a-safe-test-password\"," +
						"\"passwordConfirmation\":\"a-safe-test-password\"}", jsonHeaders()), JsonNode.class);
	}

	private String registerAndLogin() {
		ResponseEntity<JsonNode> registration = register();
		return login(registration.getBody().path("email").asText()).getBody()
				.path("accessToken").asText();
	}

	private ResponseEntity<JsonNode> login(String email) {
		return restTemplate.postForEntity("/api/v1/auth/login",
				new HttpEntity<>("{\"email\":\"" + email +
						"\",\"password\":\"a-safe-test-password\"}", jsonHeaders()), JsonNode.class);
	}

	private long createList(String token, String name) {
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/v1/lists", HttpMethod.POST,
				jsonRequest(token, "{\"name\":\"" + name + "\"}"), JsonNode.class);
		return response.getBody().path("id").asLong();
	}

	private long getFavoritesId(String token) throws Exception {
		ResponseEntity<JsonNode> lists = restTemplate.exchange("/api/v1/lists", HttpMethod.GET,
				authenticated(token), JsonNode.class);
		return lists.getBody().get(0).path("id").asLong();
	}

	private HttpEntity<String> jsonRequest(String token, String body) {
		HttpHeaders headers = jsonHeaders();
		headers.setBearerAuth(token);
		return new HttpEntity<>(body, headers);
	}

	private HttpEntity<Void> authenticated(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		return new HttpEntity<>(headers);
	}

	private HttpHeaders jsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		return headers;
	}
}
