package com.asdevs.kinematix.dietPlanner

import com.asdevs.kinematix.models.DietPlan
import com.asdevs.kinematix.models.Meal
import com.asdevs.kinematix.models.NutritionProfile


class MealRecommender {
    private val weekDays = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
    )
    fun recommendMeals(profile: NutritionProfile): List<DietPlan> {
        val selectedDays = weekDays
        val dailyCalories = calculateDailyCalories(profile)

        return when (profile.dietType) {
            "Vegetarian" -> recommendVegetarianMeals(profile, dailyCalories)
            "Vegetarian + Egg" -> recommendVegEggMeals(profile, dailyCalories)
            "Vegan" -> recommendVeganMeals(profile, dailyCalories)
            "Non-Vegetarian" -> recommendNonVegMeals(profile, dailyCalories)
            "Keto" -> recommendKetoMeals(profile, dailyCalories)
            else -> recommendBalancedMeals(profile, dailyCalories)
        }
    }

    private fun recommendVegetarianMeals(profile: NutritionProfile, dailyCalories: Int): List<DietPlan> {

        val breakfastOptions = listOf(
            Meal(
                name = "Oatmeal with Fruits",
                time = "Breakfast",
                calories = 350,
                protein = 12,
                carbs = 55,
                fats = 8,
                ingredients = listOf("Oats", "Banana", "Berries", "Almonds", "Honey"),
                instructions = "1. Cook oats with water\n2. Add sliced banana and berries\n3. Top with almonds and honey"
            ),
            Meal(
                name = "Yogurt Parfait",
                time = "Breakfast",
                calories = 300,
                protein = 15,
                carbs = 40,
                fats = 10,
                ingredients = listOf("Greek Yogurt", "Granola", "Mixed Berries", "Honey"),
                instructions = "Layer yogurt, granola, and berries. Drizzle with honey."
            ),
            Meal(
                name = "Peanut Butter Banana Toast",
                time = "Breakfast",
                calories = 320,
                protein = 10,
                carbs = 40,
                fats = 12,
                ingredients = listOf("Whole Wheat Bread", "Peanut Butter", "Banana", "Chia Seeds"),
                instructions = "1. Toast the bread\n2. Spread peanut butter\n3. Top with sliced banana and chia seeds"
            ),
            Meal(
                name = "Sprout Salad",
                time = "Breakfast",
                calories = 220,
                protein = 12,
                carbs = 35,
                fats = 3,
                ingredients = listOf("Sprouted Moong", "Tomato", "Onion", "Lemon", "Coriander"),
                instructions = "1. Mix all ingredients in a bowl\n2. Add lemon juice and mix well"
            ),
            Meal(
                name = "Besan Chilla",
                time = "Breakfast",
                calories = 280,
                protein = 10,
                carbs = 40,
                fats = 6,
                ingredients = listOf("Besan (Chickpea Flour)", "Onion", "Tomato", "Coriander", "Spices"),
                instructions = "1. Mix besan with water and spices to make a batter\n2. Add chopped onions and tomatoes\n3. Cook on a pan until golden brown"
            )

        )

        val lunchOptions = listOf(
            Meal(
                name = "Quinoa Buddha Bowl",
                time = "Lunch",
                calories = 450,
                protein = 18,
                carbs = 65,
                fats = 15,
                ingredients = listOf("Quinoa", "Chickpeas", "Sweet Potato", "Kale", "Tahini"),
                instructions = "1. Cook quinoa\n2. Roast chickpeas and sweet potato\n3. Massage kale\n4. Assemble bowl"
            ),
            Meal(
                name = "Soya Chunk Curry with Brown Rice",
                time = "Lunch",
                calories = 450,
                protein = 35,
                carbs = 50,
                fats = 10,
                ingredients = listOf("Soya Chunks", "Brown Rice", "Tomato", "Onion", "Garlic", "Spices"),
                instructions = "1. Soak soya chunks in hot water and squeeze excess water\n2. Sauté onion, tomato, and garlic with spices\n3. Add soya chunks and cook for 10 minutes\n4. Serve with brown rice"
            ),
            Meal(
                name = "Paneer Tikka with Quinoa",
                time = "Lunch",
                calories = 480,
                protein = 32,
                carbs = 50,
                fats = 15,
                ingredients = listOf("Paneer", "Quinoa", "Bell Peppers", "Yogurt", "Spices"),
                instructions = "1. Marinate paneer with yogurt and spices, then grill\n2. Cook quinoa separately\n3. Serve paneer tikka with quinoa"
            ),
            Meal(
                name = "Tofu Stir-Fry with Brown Rice",
                time = "Lunch",
                calories = 460,
                protein = 30,
                carbs = 50,
                fats = 12,
                ingredients = listOf("Tofu", "Brown Rice", "Broccoli", "Carrot", "Soy Sauce", "Garlic"),
                instructions = "1. Stir-fry tofu with garlic and vegetables\n2. Add soy sauce for flavor\n3. Serve with brown rice"
            ),
            Meal(
                name = "Palak Paneer with Roti",
                time = "Lunch",
                calories = 450,
                protein = 30,
                carbs = 45,
                fats = 12,
                ingredients = listOf("Spinach", "Paneer", "Garlic", "Tomato", "Spices", "Whole Wheat Roti"),
                instructions = "1. Cook garlic, tomato, and spinach, then blend into a puree\n2. Add paneer cubes and simmer for 5 minutes\n3. Serve with whole wheat roti"
            )

        )

        val dinnerOptions = listOf(
            Meal(
                name = "Grilled Paneer with Stir-Fried Vegetables",
                time = "Dinner",
                calories = 450,
                protein = 32,
                carbs = 40,
                fats = 15,
                ingredients = listOf("Paneer", "Bell Peppers", "Broccoli", "Olive Oil", "Spices"),
                instructions = "1. Grill paneer with spices and a little olive oil\n2. Stir-fry vegetables in a pan\n3. Serve hot"
            ),
            Meal(
                name = "Soya Bhurji with Whole Wheat Roti",
                time = "Dinner",
                calories = 420,
                protein = 38,
                carbs = 45,
                fats = 10,
                ingredients = listOf("Soya Granules", "Tomato", "Onion", "Garlic", "Spices", "Whole Wheat Roti"),
                instructions = "1. Soak and squeeze soya granules\n2. Sauté onion, tomato, and garlic with spices\n3. Add soya granules and cook for 10 minutes\n4. Serve with whole wheat roti"
            ),
            Meal(
                name = "Palak Tofu with Millets",
                time = "Dinner",
                calories = 430,
                protein = 30,
                carbs = 48,
                fats = 10,
                ingredients = listOf("Spinach", "Tofu", "Garlic", "Tomato", "Spices", "Millets"),
                instructions = "1. Cook garlic, tomato, and spinach, then blend into a puree\n2. Add tofu cubes and simmer for 5 minutes\n3. Serve with millets"
            ),
            Meal(
                name = "Methi Paneer with Quinoa",
                time = "Dinner",
                calories = 460,
                protein = 32,
                carbs = 50,
                fats = 14,
                ingredients = listOf("Paneer", "Fenugreek Leaves", "Garlic", "Tomato", "Spices", "Quinoa"),
                instructions = "1. Sauté garlic, tomato, and methi leaves\n2. Add paneer cubes and cook for a few minutes\n3. Serve with quinoa"
            ),
            Meal(
                name = "Moong Dal Soup with Whole Wheat Bread",
                time = "Dinner",
                calories = 380,
                protein = 30,
                carbs = 40,
                fats = 8,
                ingredients = listOf("Moong Dal", "Carrot", "Celery", "Garlic", "Olive Oil"),
                instructions = "1. Sauté garlic and celery in olive oil\n2. Add moong dal and carrot, cook until soft\n3. Blend slightly and serve with whole wheat bread"
            )
        )

        val snackOptions = listOf(
            Meal(
                name = "Roasted Chana and Peanuts Mix",
                time = "Snack",
                calories = 300,
                protein = 20,
                carbs = 25,
                fats = 12,
                ingredients = listOf("Roasted Chana", "Peanuts", "Himalayan Salt", "Black Pepper"),
                instructions = "1. Mix roasted chana and peanuts\n2. Add a pinch of Himalayan salt and black pepper\n3. Store in an airtight container for a quick snack"
            ),
            Meal(
                name = "Paneer Tikka Skewers",
                time = "Snack",
                calories = 320,
                protein = 28,
                carbs = 20,
                fats = 12,
                ingredients = listOf("Paneer", "Bell Peppers", "Yogurt", "Spices", "Lemon Juice"),
                instructions = "1. Marinate paneer cubes with yogurt, spices, and lemon juice\n2. Skewer them with bell peppers\n3. Grill or pan-roast until golden brown"
            ),
            Meal(
                name = "Moong Dal Chilla",
                time = "Snack",
                calories = 280,
                protein = 24,
                carbs = 30,
                fats = 6,
                ingredients = listOf("Moong Dal", "Onion", "Green Chili", "Coriander", "Spices"),
                instructions = "1. Soak moong dal and blend into a batter\n2. Add chopped onions, chili, and coriander\n3. Cook like a pancake on a tawa and serve"
            ),
            Meal(
                name = "Sprouted Moong Chaat",
                time = "Snack",
                calories = 290,
                protein = 25,
                carbs = 35,
                fats = 5,
                ingredients = listOf("Sprouted Moong", "Onion", "Tomato", "Lemon Juice", "Spices"),
                instructions = "1. Mix sprouted moong with chopped onion and tomato\n2. Add lemon juice and spices for flavor\n3. Serve fresh"
            ),
            Meal(
                name = "Boiled Chickpea Salad",
                time = "Snack",
                calories = 300,
                protein = 22,
                carbs = 40,
                fats = 5,
                ingredients = listOf("Chickpeas", "Tomato", "Cucumber", "Lemon Juice", "Chaat Masala"),
                instructions = "1. Boil chickpeas until soft\n2. Mix with chopped tomatoes and cucumber\n3. Add lemon juice and chaat masala for flavor"
            )
        )
        return generateWeeklyPlan(profile, dailyCalories, breakfastOptions, lunchOptions, dinnerOptions, snackOptions)
    }

    private fun recommendVegEggMeals(profile: NutritionProfile, dailyCalories: Int): List<DietPlan> {
        val breakfastOptions = listOf(
            Meal(
                name = "Vegetable Omelette with Cottage Cheese",
                time = "Breakfast",
                calories = 420,
                protein = 35,
                carbs = 30,
                fats = 14,
                ingredients = listOf("Eggs", "Bell Peppers", "Tomato", "Cottage Cheese", "Olive Oil", "Spices"),
                instructions = "1. Beat eggs and mix with chopped vegetables and spices\n2. Cook in olive oil until firm\n3. Top with crumbled cottage cheese"
            ),
            Meal(
                name = "Protein Pancakes with Peanut Butter",
                time = "Breakfast",
                calories = 450,
                protein = 30,
                carbs = 55,
                fats = 12,
                ingredients = listOf("Eggs", "Oats", "Banana", "Milk", "Peanut Butter"),
                instructions = "1. Blend oats, banana, eggs, and milk into a batter\n2. Cook pancakes on a pan\n3. Serve with peanut butter on top"
            ),
            Meal(
                name = "Egg and Soya Chunk Stir-Fry",
                time = "Breakfast",
                calories = 460,
                protein = 40,
                carbs = 35,
                fats = 12,
                ingredients = listOf("Eggs", "Soya Chunks", "Onion", "Bell Peppers", "Garlic", "Soy Sauce"),
                instructions = "1. Boil and squeeze soya chunks\n2. Stir-fry with onion, bell peppers, and garlic\n3. Add scrambled eggs and mix well"
            ),
            Meal(
                name = "Peanut Butter Banana Toast",
                time = "Breakfast",
                calories = 320,
                protein = 10,
                carbs = 40,
                fats = 12,
                ingredients = listOf("Whole Wheat Bread", "Peanut Butter", "Banana", "Chia Seeds"),
                instructions = "1. Toast the bread\n2. Spread peanut butter\n3. Top with sliced banana and chia seeds"
            ),
            Meal(
                name = "Besan Chilla",
                time = "Breakfast",
                calories = 280,
                protein = 10,
                carbs = 40,
                fats = 6,
                ingredients = listOf("Besan (Chickpea Flour)", "Onion", "Tomato", "Coriander", "Spices"),
                instructions = "1. Mix besan with water and spices to make a batter\n2. Add chopped onions and tomatoes\n3. Cook on a pan until golden brown"
            )
        )


        val lunchOptions = listOf(
            Meal(
                name = "Egg Fried Rice",
                time = "Lunch",
                calories = 450,
                protein = 20,
                carbs = 65,
                fats = 15,
                ingredients = listOf("Brown Rice", "Eggs", "Mixed Vegetables", "Soy Sauce", "Spring Onions"),
                instructions = "1. Cook rice\n2. Stir-fry vegetables\n3. Add beaten eggs\n4. Mix with rice and sauce"
            ),
            Meal(
                name = "Egg and Paneer Bhurji with Roti",
                time = "Lunch",
                calories = 500,
                protein = 40,
                carbs = 45,
                fats = 14,
                ingredients = listOf("Eggs", "Paneer", "Onion", "Tomato", "Green Chili", "Spices", "Whole Wheat Roti"),
                instructions = "1. Heat oil and sauté onions, tomatoes, and chilies\n2. Add scrambled eggs and crumbled paneer\n3. Cook well and serve with whole wheat roti"
            ),
            Meal(
                name = "Egg and Soya Chunk Stir-Fry with Quinoa",
                time = "Lunch",
                calories = 550,
                protein = 45,
                carbs = 50,
                fats = 12,
                ingredients = listOf("Eggs", "Soya Chunks", "Onion", "Bell Peppers", "Garlic", "Quinoa"),
                instructions = "1. Boil soya chunks and squeeze excess water\n2. Stir-fry with onions, bell peppers, and garlic\n3. Add scrambled eggs and serve with quinoa"
            ),
            Meal(
                name = "Soya Chunk Curry with Brown Rice",
                time = "Lunch",
                calories = 450,
                protein = 35,
                carbs = 50,
                fats = 10,
                ingredients = listOf("Soya Chunks", "Brown Rice", "Tomato", "Onion", "Garlic", "Spices"),
                instructions = "1. Soak soya chunks in hot water and squeeze excess water\n2. Sauté onion, tomato, and garlic with spices\n3. Add soya chunks and cook for 10 minutes\n4. Serve with brown rice"
            ),
            Meal(
                name = "Paneer Tikka with Quinoa",
                time = "Lunch",
                calories = 480,
                protein = 32,
                carbs = 50,
                fats = 15,
                ingredients = listOf("Paneer", "Quinoa", "Bell Peppers", "Yogurt", "Spices"),
                instructions = "1. Marinate paneer with yogurt and spices, then grill\n2. Cook quinoa separately\n3. Serve paneer tikka with quinoa"
            )

        )

        val dinnerOptions = listOf(
            Meal(
                name = "Egg and Lentil Soup",
                time = "Dinner",
                calories = 480,
                protein = 38,
                carbs = 45,
                fats = 10,
                ingredients = listOf("Eggs", "Lentils", "Carrots", "Garlic", "Turmeric", "Black Pepper"),
                instructions = "1. Cook lentils with carrots and garlic\n2. Add beaten eggs and mix well\n3. Season with turmeric and black pepper, serve hot"
            ),
            Meal(
                name = "Egg and Rajma Bowl",
                time = "Dinner",
                calories = 500,
                protein = 42,
                carbs = 48,
                fats = 12,
                ingredients = listOf("Eggs", "Rajma", "Onion", "Tomato", "Spices", "Brown Rice"),
                instructions = "1. Cook rajma with onions, tomatoes, and spices\n2. Add boiled eggs and mix well\n3. Serve over brown rice"
            ),
            Meal(
                name = "Egg Tofu Stir-Fry with Brown Rice",
                time = "Dinner",
                calories = 500,
                protein = 42,
                carbs = 50,
                fats = 12,
                ingredients = listOf("Eggs", "Tofu", "Bell Peppers", "Garlic", "Soy Sauce", "Brown Rice"),
                instructions = "1. Scramble eggs and keep aside\n2. Stir-fry tofu, bell peppers, and garlic with soy sauce\n3. Add scrambled eggs and serve with brown rice"
            ),
            Meal(
                name = "Grilled Paneer with Stir-Fried Vegetables",
                time = "Dinner",
                calories = 450,
                protein = 32,
                carbs = 40,
                fats = 15,
                ingredients = listOf("Paneer", "Bell Peppers", "Broccoli", "Olive Oil", "Spices"),
                instructions = "1. Grill paneer with spices and a little olive oil\n2. Stir-fry vegetables in a pan\n3. Serve hot"
            ),
            Meal(
                name = "Soya Bhurji with Whole Wheat Roti",
                time = "Dinner",
                calories = 420,
                protein = 38,
                carbs = 45,
                fats = 10,
                ingredients = listOf("Soya Granules", "Tomato", "Onion", "Garlic", "Spices", "Whole Wheat Roti"),
                instructions = "1. Soak and squeeze soya granules\n2. Sauté onion, tomato, and garlic with spices\n3. Add soya granules and cook for 10 minutes\n4. Serve with whole wheat roti"
            )
        )

        val snackOptions = listOf(
            Meal(
                name = "Egg and Cottage Cheese Wrap",
                time = "Snack",
                calories = 350,
                protein = 30,
                carbs = 30,
                fats = 10,
                ingredients = listOf("Eggs", "Whole Wheat Wrap", "Cottage Cheese", "Spinach", "Spices"),
                instructions = "1. Scramble eggs and mix with crumbled cottage cheese and spinach\n2. Spread on a whole wheat wrap and roll it up"
            ),
            Meal(
                name = "Egg and Chickpea Chaat",
                time = "Snack",
                calories = 320,
                protein = 30,
                carbs = 35,
                fats = 8,
                ingredients = listOf("Boiled Eggs", "Chickpeas", "Onion", "Tomato", "Lemon Juice", "Spices"),
                instructions = "1. Mix chopped boiled eggs with cooked chickpeas, onions, and tomatoes\n2. Add lemon juice and spices, toss well"
            ),
            Meal(
                name = "Egg and Peanut Butter Toast",
                time = "Snack",
                calories = 380,
                protein = 32,
                carbs = 40,
                fats = 12,
                ingredients = listOf("Eggs", "Whole Wheat Bread", "Peanut Butter", "Banana Slices"),
                instructions = "1. Scramble eggs and spread them over toasted bread\n2. Add peanut butter and top with banana slices"
            ),
            Meal(
                name = "Egg and Sprouts Salad",
                time = "Snack",
                calories = 310,
                protein = 32,
                carbs = 30,
                fats = 8,
                ingredients = listOf("Boiled Eggs", "Sprouts", "Cucumber", "Tomato", "Lemon Juice"),
                instructions = "1. Chop boiled eggs and mix with sprouts and vegetables\n2. Add lemon juice and mix well"
            ),
            Meal(
                name = "Moong Dal Chilla",
                time = "Snack",
                calories = 280,
                protein = 24,
                carbs = 30,
                fats = 6,
                ingredients = listOf("Moong Dal", "Onion", "Green Chili", "Coriander", "Spices"),
                instructions = "1. Soak moong dal and blend into a batter\n2. Add chopped onions, chili, and coriander\n3. Cook like a pancake on a tawa and serve"
            )
        )

        return generateWeeklyPlan(profile, dailyCalories, breakfastOptions, lunchOptions, dinnerOptions, snackOptions)
    }


    private fun recommendVeganMeals(profile: NutritionProfile, dailyCalories: Int): List<DietPlan> {
        val breakfastOptions = listOf(
            Meal(
                name = "Vegan Protein Pancakes",
                time = "Breakfast",
                calories = 420,
                protein = 30,
                carbs = 55,
                fats = 8,
                ingredients = listOf("Oats", "Banana", "Plant-Based Protein Powder", "Almond Milk", "Chia Seeds"),
                instructions = "1. Blend oats, banana, protein powder, and almond milk into a batter\n2. Cook pancakes on a non-stick pan and top with chia seeds"
            ),
            Meal(
                name = "Tofu Scramble with Whole Grain Toast",
                time = "Breakfast",
                calories = 400,
                protein = 28,
                carbs = 40,
                fats = 12,
                ingredients = listOf("Tofu", "Nutritional Yeast", "Turmeric", "Spinach", "Whole Grain Bread"),
                instructions = "1. Crumble tofu and sauté with turmeric, nutritional yeast, and spinach\n2. Serve with whole grain toast"
            ),
            Meal(
                name = "Peanut Butter and Banana Protein Smoothie",
                time = "Breakfast",
                calories = 380,
                protein = 35,
                carbs = 45,
                fats = 10,
                ingredients = listOf("Banana", "Peanut Butter", "Plant-Based Protein Powder", "Almond Milk"),
                instructions = "1. Blend all ingredients until smooth\n2. Serve chilled"
            ),
            Meal(
                name = "Vegan Lentil and Spinach Wrap",
                time = "Breakfast",
                calories = 400,
                protein = 30,
                carbs = 42,
                fats = 8,
                ingredients = listOf("Lentils", "Whole Wheat Wrap", "Spinach", "Hummus"),
                instructions = "1. Cook lentils and mix with spinach and hummus\n2. Wrap in a whole wheat tortilla"
            ),
            Meal(
                name = "Edamame and Sweet Potato Bowl",
                time = "Breakfast",
                calories = 400,
                protein = 32,
                carbs = 50,
                fats = 8,
                ingredients = listOf("Edamame", "Sweet Potato", "Spinach", "Olive Oil"),
                instructions = "1. Steam edamame and sweet potato\n2. Mix with spinach and drizzle with olive oil"
            )
        )

        val lunchOptions = listOf(
            Meal(
                name = "Vegan Buddha Bowl",
                time = "Lunch",
                calories = 460,
                protein = 36,
                carbs = 50,
                fats = 10,
                ingredients = listOf("Quinoa", "Black Beans", "Avocado", "Sweet Potato", "Spinach"),
                instructions = "1. Cook quinoa and sweet potato\n2. Add black beans, avocado, and spinach"
            ),
            Meal(
                name = "Chickpea and Spinach Curry with Whole Wheat Roti",
                time = "Lunch",
                calories = 470,
                protein = 39,
                carbs = 52,
                fats = 9,
                ingredients = listOf("Chickpeas", "Spinach", "Tomato", "Onion", "Whole Wheat Roti"),
                instructions = "1. Cook chickpeas with tomatoes, onions, and spinach\n2. Serve with whole wheat roti"
            ),
            Meal(
                name = "Quinoa and Chickpea Salad",
                time = "Lunch",
                calories = 450,
                protein = 35,
                carbs = 50,
                fats = 10,
                ingredients = listOf("Quinoa", "Chickpeas", "Cucumber", "Tomato", "Olive Oil", "Lemon Juice"),
                instructions = "1. Cook quinoa and mix with chickpeas, cucumber, and tomato\n2. Drizzle with olive oil and lemon juice"
            ),
            Meal(
                name = "Grilled Tempeh with Brown Rice",
                time = "Lunch",
                calories = 450,
                protein = 40,
                carbs = 50,
                fats = 10,
                ingredients = listOf("Tempeh", "Brown Rice", "Garlic", "Lemon Juice", "Olive Oil"),
                instructions = "1. Marinate tempeh with garlic, lemon juice, and olive oil\n2. Grill and serve with brown rice"
            ),
            Meal(
                name = "Vegan High-Protein Pasta",
                time = "Lunch",
                calories = 460,
                protein = 38,
                carbs = 54,
                fats = 9,
                ingredients = listOf("Lentil Pasta", "Tomato Sauce", "Mushrooms", "Spinach", "Olive Oil"),
                instructions = "1. Cook lentil pasta and toss with tomato sauce, mushrooms, and spinach\n2. Drizzle with olive oil"
            )
        )

        val dinnerOptions = listOf(
            Meal(
                name = "Spicy Chickpea and Quinoa Bowl",
                time = "Dinner",
                calories = 450,
                protein = 38,
                carbs = 50,
                fats = 10,
                ingredients = listOf("Quinoa", "Chickpeas", "Spinach", "Garlic", "Olive Oil"),
                instructions = "1. Cook quinoa and sauté chickpeas with garlic and spinach\n2. Mix everything and drizzle with olive oil"
            ),
            Meal(
                name = "Tofu and Broccoli Stir-Fry with Brown Rice",
                time = "Dinner",
                calories = 480,
                protein = 42,
                carbs = 55,
                fats = 12,
                ingredients = listOf("Tofu", "Broccoli", "Soy Sauce", "Brown Rice", "Sesame Seeds"),
                instructions = "1. Stir-fry tofu and broccoli with soy sauce\n2. Serve with brown rice and top with sesame seeds"
            ),
            Meal(
                name = "Lentil and Vegetable Soup",
                time = "Dinner",
                calories = 420,
                protein = 40,
                carbs = 45,
                fats = 9,
                ingredients = listOf("Lentils", "Carrots", "Celery", "Tomato", "Garlic"),
                instructions = "1. Cook lentils with carrots, celery, tomato, and garlic\n2. Simmer until thick and serve hot"
            ),
            Meal(
                name = "Stuffed Bell Peppers with Black Beans",
                time = "Dinner",
                calories = 460,
                protein = 38,
                carbs = 52,
                fats = 9,
                ingredients = listOf("Bell Peppers", "Black Beans", "Quinoa", "Tomato", "Onion"),
                instructions = "1. Mix cooked quinoa and black beans with diced tomato and onion\n2. Stuff into bell peppers and bake"
            ),
            Meal(
                name = "Vegan Tempeh Tacos",
                time = "Dinner",
                calories = 450,
                protein = 39,
                carbs = 50,
                fats = 11,
                ingredients = listOf("Tempeh", "Whole Wheat Tortillas", "Avocado", "Lettuce", "Salsa"),
                instructions = "1. Cook tempeh with spices and break into crumbles\n2. Serve in whole wheat tortillas with lettuce, avocado, and salsa"
            )
        )

        val snackOptions = listOf(
            Meal(
                name = "Peanut Butter Banana Toast",
                time = "Snack",
                calories = 250,
                protein = 10,
                carbs = 35,
                fats = 9,
                ingredients = listOf("Whole Wheat Bread", "Peanut Butter", "Banana", "Chia Seeds"),
                instructions = "1. Spread peanut butter on toasted bread\n2. Top with banana slices and chia seeds"
            ),
            Meal(
                name = "Tofu Scramble",
                time = "Snack",
                calories = 260,
                protein = 18,
                carbs = 20,
                fats = 12,
                ingredients = listOf("Tofu", "Turmeric", "Garlic", "Nutritional Yeast", "Olive Oil"),
                instructions = "1. Crumble tofu and sauté with garlic, turmeric, and nutritional yeast\n2. Cook until lightly crispy"
            ),
            Meal(
                name = "Vegan Protein Smoothie",
                time = "Snack",
                calories = 300,
                protein = 20,
                carbs = 40,
                fats = 5,
                ingredients = listOf("Plant-Based Protein Powder", "Banana", "Almond Milk", "Peanut Butter"),
                instructions = "1. Blend all ingredients together until smooth\n2. Serve chilled"
            ),
            Meal(
                name = "Roasted Chickpeas",
                time = "Snack",
                calories = 200,
                protein = 12,
                carbs = 30,
                fats = 5,
                ingredients = listOf("Chickpeas", "Olive Oil", "Paprika", "Garlic Powder", "Salt"),
                instructions = "1. Toss chickpeas with olive oil and spices\n2. Roast at 200°C for 20-25 minutes"
            )

        )

        return generateWeeklyPlan(profile, dailyCalories, breakfastOptions, lunchOptions, dinnerOptions, snackOptions)
    }


    private fun recommendNonVegMeals(profile: NutritionProfile, dailyCalories: Int): List<DietPlan> {
        val breakfastOptions = listOf(
            Meal(
                name = "Chicken Breakfast Burrito",
                time = "Breakfast",
                calories = 450,
                protein = 30,
                carbs = 40,
                fats = 20,
                ingredients = listOf("Eggs", "Chicken Breast", "Tortilla", "Cheese", "Bell Peppers", "Onions"),
                instructions = "1. Cook chicken\n2. Scramble eggs\n3. Assemble burrito with vegetables and cheese"
            ),
            Meal(
                name = "Protein Pancakes with Peanut Butter",
                time = "Breakfast",
                calories = 450,
                protein = 38,
                carbs = 55,
                fats = 12,
                ingredients = listOf("Oats", "Egg Whites", "Banana", "Protein Powder", "Peanut Butter"),
                instructions = "1. Blend oats, egg whites, banana, and protein powder into a batter\n2. Cook pancakes and top with peanut butter"
            ),
            Meal(
                name = "Scrambled Eggs with Chicken Sausage",
                time = "Breakfast",
                calories = 450,
                protein = 38,
                carbs = 15,
                fats = 25,
                ingredients = listOf("Eggs", "Chicken Sausage", "Olive Oil", "Salt", "Pepper"),
                instructions = "1. Scramble eggs with salt and pepper\n2. Cook chicken sausage and serve together"
            ),
            Meal(
                name = "Chicken and Sweet Potato Hash",
                time = "Breakfast",
                calories = 470,
                protein = 42,
                carbs = 50,
                fats = 12,
                ingredients = listOf("Chicken Breast", "Sweet Potato", "Onion", "Garlic", "Olive Oil"),
                instructions = "1. Cook diced chicken and sweet potatoes with onion and garlic\n2. Serve hot"
            ),
            Meal(
                name = "Greek Yogurt with Honey and Nuts",
                time = "Breakfast",
                calories = 400,
                protein = 35,
                carbs = 40,
                fats = 10,
                ingredients = listOf("Greek Yogurt", "Honey", "Almonds", "Walnuts", "Chia Seeds"),
                instructions = "1. Mix Greek yogurt with honey\n2. Top with almonds, walnuts, and chia seeds"
            )
        )

        val lunchOptions = listOf(
            Meal(
                name = "Grilled Chicken Salad",
                time = "Lunch",
                calories = 450,
                protein = 35,
                carbs = 20,
                fats = 25,
                ingredients = listOf("Chicken Breast", "Mixed Greens", "Avocado", "Cherry Tomatoes", "Olive Oil", "Balsamic"),
                instructions = "1. Grill chicken\n2. Prepare salad\n3. Add dressing"
            ),
            Meal(
                name = "Grilled Chicken with Quinoa and Vegetables",
                time = "Lunch",
                calories = 550,
                protein = 50,
                carbs = 45,
                fats = 15,
                ingredients = listOf("Chicken Breast", "Quinoa", "Broccoli", "Carrots", "Olive Oil", "Garlic"),
                instructions = "1. Grill chicken with garlic and olive oil\n2. Cook quinoa and serve with steamed vegetables"
            ),
            Meal(
                name = "Egg and Chicken Rice Bowl",
                time = "Lunch",
                calories = 580,
                protein = 52,
                carbs = 50,
                fats = 16,
                ingredients = listOf("Chicken Breast", "Boiled Egg", "Brown Rice", "Avocado", "Soy Sauce"),
                instructions = "1. Grill chicken and slice boiled egg\n2. Serve over brown rice with avocado and soy sauce"
            ),
            Meal(
                name = "Grilled Fish with Mashed Sweet Potatoes",
                time = "Lunch",
                calories = 550,
                protein = 50,
                carbs = 40,
                fats = 14,
                ingredients = listOf("Fish Fillet", "Sweet Potatoes", "Olive Oil", "Garlic", "Spinach"),
                instructions = "1. Grill fish with olive oil and garlic\n2. Serve with mashed sweet potatoes and sautéed spinach"
            ),
            Meal(
                name = "Omelette with Smoked Salmon and Spinach",
                time = "Lunch",
                calories = 520,
                protein = 48,
                carbs = 20,
                fats = 22,
                ingredients = listOf("Eggs", "Smoked Salmon", "Spinach", "Olive Oil", "Cheese"),
                instructions = "1. Beat eggs and cook in a pan\n2. Add smoked salmon, spinach, and cheese, then fold into an omelette"
            )
        )

        val dinnerOptions = listOf(
            Meal(
                name = "Grilled Salmon with Roasted Vegetables",
                time = "Dinner",
                calories = 580,
                protein = 50,
                carbs = 40,
                fats = 18,
                ingredients = listOf("Salmon", "Zucchini", "Bell Peppers", "Olive Oil", "Garlic"),
                instructions = "1. Grill salmon with olive oil and garlic\n2. Roast zucchini and bell peppers, then serve together"
            ),
            Meal(
                name = "Chicken Breast with Quinoa and Spinach",
                time = "Dinner",
                calories = 550,
                protein = 52,
                carbs = 45,
                fats = 12,
                ingredients = listOf("Chicken Breast", "Quinoa", "Spinach", "Garlic", "Olive Oil"),
                instructions = "1. Grill chicken breast\n2. Cook quinoa and serve with sautéed spinach"
            ),
            Meal(
                name = "Chicken and Sweet Potato Mash",
                time = "Dinner",
                calories = 560,
                protein = 50,
                carbs = 45,
                fats = 12,
                ingredients = listOf("Chicken Breast", "Sweet Potatoes", "Garlic", "Olive Oil", "Paprika"),
                instructions = "1. Grill chicken breast\n2. Mash sweet potatoes with garlic and paprika"
            ),
            Meal(
                name = "Egg Omelette with Smoked Salmon",
                time = "Dinner",
                calories = 490,
                protein = 45,
                carbs = 15,
                fats = 22,
                ingredients = listOf("Eggs", "Smoked Salmon", "Spinach", "Olive Oil", "Cheese"),
                instructions = "1. Beat eggs and cook in a pan\n2. Add smoked salmon, spinach, and cheese, then fold into an omelette"
            ),
            Meal(
                name = "Boiled Eggs with Grilled Chicken Salad",
                time = "Dinner",
                calories = 500,
                protein = 50,
                carbs = 20,
                fats = 16,
                ingredients = listOf("Boiled Eggs", "Grilled Chicken", "Lettuce", "Olive Oil", "Parmesan Cheese"),
                instructions = "1. Slice boiled eggs and grilled chicken\n2. Toss with lettuce, olive oil, and Parmesan cheese"
            )
        )

        val snackOptions = listOf(
            Meal(
                name = "Egg and Tuna Lettuce Wraps",
                time = "Snack",
                calories = 260,
                protein = 28,
                carbs = 6,
                fats = 14,
                ingredients = listOf("Boiled Eggs", "Tuna", "Lettuce", "Greek Yogurt", "Black Pepper"),
                instructions = "1. Mash boiled eggs with tuna and Greek yogurt\n2. Wrap in lettuce leaves and season with black pepper"
            ),
            Meal(
                name = "Greek Yogurt with Grilled Chicken",
                time = "Snack",
                calories = 290,
                protein = 30,
                carbs = 20,
                fats = 10,
                ingredients = listOf("Greek Yogurt", "Grilled Chicken", "Cucumber", "Lemon Juice"),
                instructions = "1. Dice grilled chicken and mix with Greek yogurt\n2. Add cucumber slices and lemon juice"
            ),
            Meal(
                name = "Hard-Boiled Eggs with Almond Butter",
                time = "Snack",
                calories = 280,
                protein = 24,
                carbs = 8,
                fats = 16,
                ingredients = listOf("Boiled Eggs", "Almond Butter", "Black Pepper"),
                instructions = "1. Slice boiled eggs in half\n2. Spread almond butter and sprinkle with black pepper"
            ),
            Meal(
                name = "Grilled Chicken Strips",
                time = "Snack",
                calories = 270,
                protein = 30,
                carbs = 5,
                fats = 12,
                ingredients = listOf("Chicken Breast", "Olive Oil", "Garlic Powder", "Paprika"),
                instructions = "1. Cut chicken breast into strips\n2. Grill with olive oil, garlic powder, and paprika"
            ),
            Meal(
                name = "Boiled Eggs with Hummus",
                time = "Snack",
                calories = 250,
                protein = 22,
                carbs = 10,
                fats = 12,
                ingredients = listOf("Boiled Eggs", "Hummus", "Paprika"),
                instructions = "1. Slice boiled eggs in half\n2. Serve with hummus and sprinkle paprika on top"
            )
        )

        return generateWeeklyPlan(profile, dailyCalories, breakfastOptions, lunchOptions, dinnerOptions, snackOptions)
    }

    private fun recommendKetoMeals(profile: NutritionProfile, dailyCalories: Int): List<DietPlan> {
        val breakfastOptions = listOf(
            Meal(
                name = "Keto Breakfast Bowl",
                time = "Breakfast",
                calories = 450,
                protein = 25,
                carbs = 5,
                fats = 38,
                ingredients = listOf("Eggs", "Avocado", "Bacon", "Spinach", "Cheese"),
                instructions = "1. Cook bacon\n2. Scramble eggs\n3. Add avocado and cheese"
            ),
            Meal(
                name = "Cheese Omelette with Avocado",
                time = "Breakfast",
                calories = 450,
                protein = 30,
                carbs = 6,
                fats = 35,
                ingredients = listOf("Eggs", "Cheese", "Butter", "Avocado"),
                instructions = "1. Beat eggs and cook in butter\n2. Add cheese and fold\n3. Serve with sliced avocado"
            ),
            Meal(
                name = "Smoked Salmon and Cream Cheese Wrap",
                time = "Breakfast",
                calories = 480,
                protein = 38,
                carbs = 5,
                fats = 32,
                ingredients = listOf("Smoked Salmon", "Cream Cheese", "Lettuce"),
                instructions = "1. Spread cream cheese on lettuce leaves\n2. Add smoked salmon and roll up"
            ),
            Meal(
                name = "Keto Protein Pancakes",
                time = "Breakfast",
                calories = 400,
                protein = 35,
                carbs = 6,
                fats = 28,
                ingredients = listOf("Almond Flour", "Eggs", "Whey Protein", "Butter"),
                instructions = "1. Mix almond flour, eggs, and whey protein\n2. Cook in butter and serve"
            ),
            Meal(
                name = "Keto Chia Seed Pudding",
                time = "Breakfast",
                calories = 380,
                protein = 22,
                carbs = 8,
                fats = 30,
                ingredients = listOf("Chia Seeds", "Almond Milk", "Protein Powder", "Stevia"),
                instructions = "1. Mix chia seeds with almond milk and protein powder\n2. Refrigerate overnight and serve"
            )

        )

        val lunchOptions = listOf(
            Meal(
                name = "Chicken Caesar Salad",
                time = "Lunch",
                calories = 500,
                protein = 35,
                carbs = 5,
                fats = 40,
                ingredients = listOf("Chicken Breast", "Romaine Lettuce", "Parmesan", "Caesar Dressing", "Bacon Bits"),
                instructions = "1. Grill chicken\n2. Assemble salad\n3. Add dressing"
            ),
            Meal(
                name = "Tuna Avocado Boats",
                time = "Lunch",
                calories = 450,
                protein = 30,
                carbs = 6,
                fats = 35,
                ingredients = listOf("Tuna", "Avocado", "Mayo", "Celery", "Herbs"),
                instructions = "1. Mix tuna with mayo\n2. Fill avocado halves"
            ),
            Meal(
                name = "Grilled Chicken with Spinach and Cheese",
                time = "Lunch",
                calories = 520,
                protein = 50,
                carbs = 8,
                fats = 30,
                ingredients = listOf("Chicken Breast", "Spinach", "Cheese", "Olive Oil"),
                instructions = "1. Grill chicken with olive oil\n2. Sauté spinach with cheese and serve"
            ),
            Meal(
                name = "Grilled Salmon with Creamy Broccoli",
                time = "Lunch",
                calories = 600,
                protein = 55,
                carbs = 9,
                fats = 38,
                ingredients = listOf("Salmon", "Broccoli", "Heavy Cream", "Butter"),
                instructions = "1. Bake salmon with butter\n2. Cook broccoli with heavy cream and season"
            ),
            Meal(
                name = "Egg Salad Lettuce Wraps",
                time = "Lunch",
                calories = 450,
                protein = 38,
                carbs = 5,
                fats = 32,
                ingredients = listOf("Boiled Eggs", "Mayonnaise", "Lettuce", "Olive Oil"),
                instructions = "1. Mash boiled eggs with mayonnaise and olive oil\n2. Serve in lettuce wraps"
            )
        )

        val dinnerOptions = listOf(
            Meal(
                name = "Baked Salmon with Vegetables",
                time = "Dinner",
                calories = 550,
                protein = 40,
                carbs = 8,
                fats = 42,
                ingredients = listOf("Salmon", "Asparagus", "Butter", "Lemon", "Herbs"),
                instructions = "1. Season salmon\n2. Bake with asparagus\n3. Add butter sauce"
            ),
            Meal(
                name = "Cauliflower Rice Stir-Fry",
                time = "Dinner",
                calories = 400,
                protein = 30,
                carbs = 10,
                fats = 28,
                ingredients = listOf("Ground Beef", "Cauliflower Rice", "Bell Peppers", "Soy Sauce", "Sesame Oil"),
                instructions = "1. Cook beef\n2. Stir-fry cauliflower rice\n3. Combine with sauce"
            ),
            Meal(
                name = "Garlic Butter Steak with Asparagus",
                time = "Dinner",
                calories = 600,
                protein = 55,
                carbs = 7,
                fats = 40,
                ingredients = listOf("Steak", "Butter", "Garlic", "Asparagus"),
                instructions = "1. Grill steak with garlic butter\n2. Sauté asparagus in butter and serve"
            ),
            Meal(
                name = "Keto Chicken Parmesan",
                time = "Dinner",
                calories = 580,
                protein = 52,
                carbs = 8,
                fats = 38,
                ingredients = listOf("Chicken Breast", "Parmesan Cheese", "Tomato Sauce", "Olive Oil"),
                instructions = "1. Coat chicken in parmesan and bake\n2. Serve with low-carb tomato sauce"
            ),
            Meal(
                name = "Keto Meatballs with Marinara Sauce",
                time = "Dinner",
                calories = 570,
                protein = 48,
                carbs = 6,
                fats = 38,
                ingredients = listOf("Ground Beef", "Parmesan Cheese", "Eggs", "Tomato Sauce", "Olive Oil"),
                instructions = "1. Mix ground beef with parmesan and eggs, shape into meatballs\n2. Cook in tomato sauce and serve"
            )
        )

        val snackOptions = listOf(
            Meal(
                name = "Keto Fat Bombs",
                time = "Snack",
                calories = 200,
                protein = 4,
                carbs = 2,
                fats = 20,
                ingredients = listOf("Cream Cheese", "Coconut Oil", "Nuts", "Stevia"),
                instructions = "1. Mix ingredients\n2. Freeze in molds"
            ),
            Meal(
                name = "Cheese and Pepperoni",
                time = "Snack",
                calories = 180,
                protein = 12,
                carbs = 1,
                fats = 15,
                ingredients = listOf("Cheese Cubes", "Pepperoni", "Olives"),
                instructions = "Arrange on plate and serve"
            ),
            Meal(
                name = "Cheese and Almonds",
                time = "Snack",
                calories = 300,
                protein = 18,
                carbs = 6,
                fats = 24,
                ingredients = listOf("Cheddar Cheese", "Almonds"),
                instructions = "1. Slice cheddar cheese\n2. Pair with almonds for a crunchy snack"
            ),
            Meal(
                name = "Boiled Eggs with Avocado",
                time = "Snack",
                calories = 280,
                protein = 22,
                carbs = 4,
                fats = 20,
                ingredients = listOf("Boiled Eggs", "Avocado", "Salt", "Pepper"),
                instructions = "1. Slice boiled eggs and avocado\n2. Sprinkle with salt and pepper"
            ),
            Meal(
                name = "Cottage Cheese with Flaxseeds",
                time = "Snack",
                calories = 290,
                protein = 25,
                carbs = 4,
                fats = 18,
                ingredients = listOf("Cottage Cheese", "Flaxseeds"),
                instructions = "1. Mix flaxseeds into cottage cheese\n2. Enjoy as a quick, high-protein snack"
            )
        )

        return generateWeeklyPlan(profile, dailyCalories, breakfastOptions, lunchOptions, dinnerOptions, snackOptions)
    }

    private fun recommendBalancedMeals(profile: NutritionProfile, dailyCalories: Int): List<DietPlan> {
        val breakfastOptions = listOf(
            Meal(
                name = "Classic Breakfast",
                time = "Breakfast",
                calories = 400,
                protein = 20,
                carbs = 45,
                fats = 15,
                ingredients = listOf("Whole Grain Bread", "Eggs", "Avocado", "Cherry Tomatoes", "Greek Yogurt"),
                instructions = "1. Toast bread\n2. Cook eggs to preference\n3. Slice avocado\n4. Serve with yogurt"
            ),
            Meal(
                name = "Protein Oatmeal",
                time = "Breakfast",
                calories = 350,
                protein = 25,
                carbs = 45,
                fats = 10,
                ingredients = listOf("Oats", "Protein Powder", "Banana", "Almond Milk", "Chia Seeds", "Honey"),
                instructions = "1. Cook oats with almond milk\n2. Stir in protein powder\n3. Top with banana and seeds"
            ),
            Meal(
                name = "Breakfast Wrap",
                time = "Breakfast",
                calories = 420,
                protein = 22,
                carbs = 48,
                fats = 18,
                ingredients = listOf("Whole Wheat Tortilla", "Scrambled Eggs", "Spinach", "Bell Peppers", "Cheese", "Salsa"),
                instructions = "1. Scramble eggs\n2. Warm tortilla\n3. Add fillings and wrap"
            ),
            Meal(
                name = "Oatmeal with Peanut Butter and Banana",
                time = "Breakfast",
                calories = 420,
                protein = 18,
                carbs = 55,
                fats = 12,
                ingredients = listOf("Oats", "Banana", "Peanut Butter", "Almond Milk", "Cinnamon"),
                instructions = "1. Cook oats with almond milk\n2. Top with sliced banana, peanut butter, and cinnamon"
            ),
            Meal(
                name = "Smoothie Bowl with Protein Powder",
                time = "Breakfast",
                calories = 390,
                protein = 30,
                carbs = 50,
                fats = 8,
                ingredients = listOf("Banana", "Berries", "Protein Powder", "Almond Milk", "Chia Seeds"),
                instructions = "1. Blend banana, berries, protein powder, and almond milk\n2. Pour into a bowl and top with chia seeds"
            )
        )

        val lunchOptions = listOf(
            Meal(
                name = "Power Bowl",
                time = "Lunch",
                calories = 450,
                protein = 30,
                carbs = 55,
                fats = 15,
                ingredients = listOf("Brown Rice", "Grilled Chicken", "Mixed Vegetables", "Chickpeas", "Tahini Dressing"),
                instructions = "1. Cook rice\n2. Grill chicken\n3. Steam vegetables\n4. Assemble bowl"
            ),
            Meal(
                name = "Grilled Paneer with Quinoa and Veggies",
                time = "Lunch",
                calories = 520,
                protein = 35,
                carbs = 55,
                fats = 15,
                ingredients = listOf("Paneer", "Quinoa", "Bell Peppers", "Olive Oil", "Garlic"),
                instructions = "1. Grill paneer with olive oil and garlic\n2. Serve with cooked quinoa and sautéed bell peppers"
            ),
            Meal(
                name = "Asian Stir-Fry",
                time = "Lunch",
                calories = 420,
                protein = 28,
                carbs = 45,
                fats = 16,
                ingredients = listOf("Brown Rice", "Tofu", "Broccoli", "Carrots", "Snap Peas", "Stir-Fry Sauce"),
                instructions = "1. Cook rice\n2. Stir-fry tofu and vegetables\n3. Add sauce"
            ),
            Meal(
                name = "Vegetable Wrap with Hummus",
                time = "Lunch",
                calories = 480,
                protein = 25,
                carbs = 55,
                fats = 12,
                ingredients = listOf("Whole Wheat Tortilla", "Hummus", "Cucumber", "Carrots", "Spinach"),
                instructions = "1. Spread hummus on a whole wheat tortilla\n2. Add sliced cucumber, carrots, and spinach, then wrap"
            ),
            Meal(
                name = "Grilled Chicken with Sweet Potatoes",
                time = "Lunch",
                calories = 550,
                protein = 40,
                carbs = 55,
                fats = 12,
                ingredients = listOf("Chicken Breast", "Sweet Potatoes", "Olive Oil", "Rosemary"),
                instructions = "1. Grill chicken breast with olive oil and rosemary\n2. Serve with baked sweet potatoes"
            )
        )

        val dinnerOptions = listOf(
            Meal(
                name = "Baked Fish Dinner",
                time = "Dinner",
                calories = 440,
                protein = 35,
                carbs = 40,
                fats = 18,
                ingredients = listOf("White Fish", "Sweet Potato", "Asparagus", "Lemon", "Herbs", "Olive Oil"),
                instructions = "1. Season fish\n2. Bake fish and vegetables\n3. Serve with roasted sweet potato"
            ),
            Meal(
                name = "Grilled Chicken with Sweet Potato Mash",
                time = "Dinner",
                calories = 530,
                protein = 42,
                carbs = 50,
                fats = 12,
                ingredients = listOf("Chicken Breast", "Sweet Potatoes", "Olive Oil", "Garlic", "Rosemary"),
                instructions = "1. Grill chicken with olive oil, garlic, and rosemary\n2. Mash sweet potatoes and serve"
            ),
            Meal(
                name = "Vegetable Pasta",
                time = "Dinner",
                calories = 420,
                protein = 18,
                carbs = 65,
                fats = 14,
                ingredients = listOf("Whole Grain Pasta", "Mixed Vegetables", "Marinara Sauce", "Parmesan", "Olive Oil", "Basil"),
                instructions = "1. Cook pasta\n2. Sauté vegetables\n3. Combine with sauce\n4. Top with cheese"
            ),
            Meal(
                name = "Greek Yogurt Bowl with Nuts and Seeds",
                time = "Dinner",
                calories = 450,
                protein = 40,
                carbs = 40,
                fats = 12,
                ingredients = listOf("Greek Yogurt", "Almonds", "Walnuts", "Chia Seeds", "Honey"),
                instructions = "1. Mix Greek yogurt with almonds, walnuts, and chia seeds\n2. Drizzle with honey and serve"
            ),
            Meal(
                name = "Lentil Soup with Roasted Vegetables",
                time = "Dinner",
                calories = 470,
                protein = 30,
                carbs = 50,
                fats = 12,
                ingredients = listOf("Lentils", "Carrots", "Bell Peppers", "Olive Oil", "Garlic"),
                instructions = "1. Cook lentils with carrots and garlic\n2. Roast bell peppers in olive oil and serve with the soup"
            )
        )

        val snackOptions = listOf(
            Meal(
                name = "Protein Power Snack",
                time = "Snack",
                calories = 180,
                protein = 12,
                carbs = 15,
                fats = 10,
                ingredients = listOf("Apple", "Peanut Butter", "Greek Yogurt"),
                instructions = "Serve apple slices with peanut butter and yogurt"
            ),
            Meal(
                name = "Energy Mix",
                time = "Snack",
                calories = 200,
                protein = 8,
                carbs = 22,
                fats = 12,
                ingredients = listOf("Mixed Nuts","Dried Fruit", "Dark Chocolate Chips"),
                instructions = "Mix all ingredients in a small portion"
            ),
            Meal(
                name = "Cottage Cheese with Pineapple and Chia Seeds",
                time = "Snack",
                calories = 280,
                protein = 25,
                carbs = 35,
                fats = 6,
                ingredients = listOf("Cottage Cheese", "Pineapple", "Chia Seeds", "Cinnamon"),
                instructions = "1. Mix cottage cheese with chopped pineapple\n2. Sprinkle chia seeds and cinnamon on top"
            ),
            Meal(
                name = "Peanut Butter and Banana on Whole Wheat Toast",
                time = "Snack",
                calories = 300,
                protein = 15,
                carbs = 40,
                fats = 10,
                ingredients = listOf("Whole Wheat Bread", "Peanut Butter", "Banana", "Chia Seeds"),
                instructions = "1. Spread peanut butter on whole wheat toast\n2. Top with banana slices and chia seeds"
            ),
            Meal(
                name = "Roasted Chickpeas with Spices",
                time = "Snack",
                calories = 270,
                protein = 20,
                carbs = 35,
                fats = 6,
                ingredients = listOf("Chickpeas", "Olive Oil", "Paprika", "Garlic Powder"),
                instructions = "1. Toss chickpeas with olive oil and spices\n2. Roast until crispy and serve"
            )
        )

        return generateWeeklyPlan(
            profile = profile,
            dailyCalories = dailyCalories,
            breakfastOptions = breakfastOptions,
            lunchOptions = lunchOptions,
            dinnerOptions = dinnerOptions,
            snackOptions = snackOptions
        )
    }

    private fun generateWeeklyPlan(
        profile: NutritionProfile,
        dailyCalories: Int,
        breakfastOptions: List<Meal>,
        lunchOptions: List<Meal>,
        dinnerOptions: List<Meal>,
        snackOptions: List<Meal>
    ): List<DietPlan> {
        val plans = mutableListOf<DietPlan>()

        weekDays.forEach { day ->
            val dailyMeals = mutableListOf<Meal>()

            when (profile.mealsPerDay) {
                3 -> {
                    dailyMeals.add(breakfastOptions.random())
                    dailyMeals.add(lunchOptions.random())
                    dailyMeals.add(dinnerOptions.random())
                }
                4 -> {
                    dailyMeals.add(breakfastOptions.random())
                    dailyMeals.add(lunchOptions.random())
                    dailyMeals.add(snackOptions.random())
                    dailyMeals.add(dinnerOptions.random())
                }
                5 -> {
                    dailyMeals.add(breakfastOptions.random())
                    dailyMeals.add(snackOptions.random())
                    dailyMeals.add(lunchOptions.random())
                    dailyMeals.add(snackOptions.random())
                    dailyMeals.add(dinnerOptions.random())
                }
                else -> {
                    // Default to 3 meals if not specified
                    dailyMeals.add(breakfastOptions.random())
                    dailyMeals.add(lunchOptions.random())
                    dailyMeals.add(dinnerOptions.random())
                }
            }

            plans.add(
                DietPlan(
                    date = day,
                    meals = dailyMeals,
                    totalCalories = dailyMeals.sumOf { it.calories },
                    totalProtein = dailyMeals.sumOf { it.protein },
                    totalCarbs = dailyMeals.sumOf { it.carbs },
                    totalFats = dailyMeals.sumOf { it.fats }
                )
            )
        }

        return plans
    }

    private fun calculateDailyCalories(profile: NutritionProfile): Int {
        val bmr = if (profile.gender == "Male") {
            88.362 + (13.397 * profile.weight) + (4.799 * profile.height) - (5.677 * profile.age)
        } else {
            447.593 + (9.247 * profile.weight) + (3.098 * profile.height) - (4.330 * profile.age)
        }

        val activityMultiplier = when (profile.activityLevel) {
            "Sedentary" -> 1.2
            "Lightly Active" -> 1.375
            "Moderately Active" -> 1.55
            "Very Active" -> 1.725
            "Extra Active" -> 1.9
            else -> 1.2
        }

        var calories = (bmr * activityMultiplier).toInt()

        calories = when (profile.goal) {
            "Weight Loss" -> (calories * 0.8).toInt()
            "Muscle Gain" -> (calories * 1.1).toInt()
            else -> calories
        }

        return calories
    }
}