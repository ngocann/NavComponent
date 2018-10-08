# NavComponent
NavComponent is lightweight library, use Annotation Processor to generate static class startActivity for Activity. It help use remove some boilerplate code. 

Without NavComponent
```java
public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, DetailActivity.class);
        context.startActivity(intent);
    }
}
```

Use NavComponent
```java
@NavComponent
public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavComponents.bind(this);
    }
}
```

To startActivity:
```java
        NavComponents.start(this, DetailActivity.class);
```

Add Extra to Activity 
```java
@NavComponent
public class DetailActivity extends AppCompatActivity {
   @Extra String username;
   @Extra Integer value = 0;   
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavComponents.bind(this);
    }
}
```

To startActivity with extra:
```java
        NavComponents.start(this, DetailActivity.class, "username", 13);
```

#Getting Parceler
```
    implementation "me.blackdroid.annotation:annotation:1.1.1"
    kapt "me.blackdroid.processor:processor:1.1.1"
```
