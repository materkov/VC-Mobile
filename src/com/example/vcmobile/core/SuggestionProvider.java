package com.example.vcmobile.core;

import android.content.SearchRecentSuggestionsProvider;

public class SuggestionProvider extends SearchRecentSuggestionsProvider { 
	public static final String AUTHORITY = SuggestionProvider.class.getName();

	public static final int MODE = DATABASE_MODE_QUERIES;

	public SuggestionProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}
}
