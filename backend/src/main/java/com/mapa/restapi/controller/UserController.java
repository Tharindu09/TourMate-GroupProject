package com.mapa.restapi.controller;

import com.mapa.restapi.dto.UserDto;
import com.mapa.restapi.model.TouristAttraction;
import com.mapa.restapi.model.User;
import com.mapa.restapi.model.UserPlan;
import com.mapa.restapi.service.BookmarkPlaceService;
import com.mapa.restapi.service.UserPlanService;
import com.mapa.restapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins="*")//allow for all the ports
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private BookmarkPlaceService bookmarkPlaceService;

    @Autowired
    private UserPlanService userPlanService;

    @Autowired
    private UserService userService;

    @PostMapping("/addbookmarks")
    public ResponseEntity<?> addBookmarks(@RequestBody TouristAttraction place , @AuthenticationPrincipal UserDetails userDetails){

        String username = userDetails.getUsername();
        int code = bookmarkPlaceService.addBookmark(username,place);
        if (code==0){
            return ResponseEntity.ok().body("Bookmark added successfully");
        }
        return ResponseEntity.badRequest().body("Bookmark could not be added");
    }

    @PostMapping("/removebookmark")
    public ResponseEntity<?> removeBookmarks(@RequestBody TouristAttraction place){

        int code = bookmarkPlaceService.removeBookmark(place);
        if (code==0){
            return ResponseEntity.ok().body("Bookmark removed");
        }
        return ResponseEntity.badRequest().body("Error while removing bookmark");
    }

    //Get Bookmark Id
    @GetMapping("/getbookmarks")
    public ResponseEntity<?> getBookmarks(@AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails.getUsername();
        List<Long> code = bookmarkPlaceService.getBookmarksId(username);
        return ResponseEntity.ok().body(code);
    }

    @PostMapping("/create-plan")
    public ResponseEntity<?> createPlan(@RequestBody UserPlan plan, @AuthenticationPrincipal UserDetails userDetails){

        String username = userDetails.getUsername();

        userPlanService.addPlan(plan,username);

        return ResponseEntity.ok("Plan created");

    }

    @PostMapping("/addPlan")
    public ResponseEntity<?> addPlan(@RequestBody UserPlan plan,@AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails.getUsername();
        userPlanService.addPlan(plan,username);
        return ResponseEntity.ok().body("Plan added");


    }

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getUserProfileByEmail(@RequestParam String email) {
        Optional<User> user = userService.getUserByEmail(email);
        if (user.isPresent()) {
            UserDto dto = new UserDto();
            dto.setUserid(user.get().getUserid());
            dto.setFirstname(user.get().getFirstname());
            dto.setLastname(user.get().getLastname());
            dto.setEmail(user.get().getEmail());
            dto.setIdentifier(user.get().getIdentifier());
            dto.setUsertype(user.get().getUsertype());
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    // Update user profile by email
    @PutMapping("/profile")
    public ResponseEntity<User> updateUserProfileByEmail(@RequestParam String email, @RequestBody User updatedUser) {
        try {
            User user = userService.updateUserByEmail(email, updatedUser); // Update service method to update by email
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


}