package yoyoyousei.twitter.clone.app;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import yoyoyousei.twitter.clone.domain.model.Tweet;
import yoyoyousei.twitter.clone.domain.model.User;
import yoyoyousei.twitter.clone.domain.service.*;
import yoyoyousei.twitter.clone.util.Util;

import javax.persistence.Entity;
import java.security.Principal;
import java.util.*;

//import yoyoyousei.twitter.clone.domain.service.UserService;

/**
 * Created by s-sumi on 2017/02/28.
 */
@Controller
//@SessionAttributes(value = {"userinfo"})
public class TwitterCloneController {

    public static final Logger log=LoggerFactory.getLogger(TwitterCloneController.class);

    @Autowired
    TweetService tweetService;

    @Autowired
    UserService userService;

    @Autowired
    TwitterCloneUserDetailsService userDetailsService;

    @GetMapping(value= "/")
    String timeline(Principal principal,Model model){
        model.addAttribute("tweetForm",new TweetForm());    //attribute can be omitted.
                                                            //default attribute name is Classname whose first letter is lower case.
        model.addAttribute("tweets",tweetService.findAllDesc());
        model.addAttribute("tweet",new Tweet());

        model.addAttribute("userinfo", Util.getUserFromPrincipal(principal));
        return "timeline";
    }


    @PostMapping(value = "/")
    String tweet(@Validated TweetForm form, BindingResult bindingResult,Model model){
        if(bindingResult.hasErrors()){
            Set<String> err=new HashSet<>();
            bindingResult.getAllErrors().forEach(e->err.add(e.getDefaultMessage()));
            model.addAttribute("errors",err);
            //return timeline(principal,model);
            return "redirect:/";
        }
        Tweet tweet=new Tweet(form.getContent());

        //tweetService.save(tweet);
        try{
            tweetService.save(tweet);
        }catch (Exception e){
            Set<String> err=new HashSet<>();
            err.add("an error occured. try again.");
            model.addAttribute("errors",err);
            //return timeline(principal,model);
            return "redirect:/";
        }

        return "redirect:/";
    }


    //loginはspring securityに委譲

    //register
    @GetMapping(value = "/register")
    String registerPage(Model model){
        model.addAttribute("registerForm",new RegisterForm());
        return "register";
    }
    @PostMapping(value = "/register")
    String register(@Validated RegisterForm form, BindingResult bindingResult, Model model){
        if(bindingResult.hasErrors()){
            log.info("user:"+form.getUserId());
            log.info("pass:"+form.getPassword());
            log.info("scr:"+form.getScreenName());
            Set<String> err=new HashSet<>();
            bindingResult.getAllErrors().forEach(e->err.add(e.getDefaultMessage()));
            model.addAttribute("errors",err);
            return "register";
        }

        log.info("user:"+form.getUserId());
        log.info("pass:"+form.getPassword());
        log.info("scr:"+form.getScreenName());

        BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
        User user=new User(form.getUserId(),encoder.encode(form.getPassword()),form.getScreenName());
        try{
            userService.create(user);
        }catch (UserIdAlreadyExistsException e){
            Set<String> errors=new HashSet<>();
            errors.add(e.getMessage());
            model.addAttribute("errors",errors);
            return "register";
        }catch (Exception e){

            Set<String> errors=new HashSet<>();
            errors.add("unexpected error occured. try again.");
            model.addAttribute("errors",errors);

            log.info(e.toString());
            return "register";
        }
        return "redirect:/loginForm";
    }

    @GetMapping("/modify")
    String modifyUserDataPage(Model model){
        model.addAttribute("userForm",new UserForm());
        return "mypage";
    }
    @PostMapping("/modify")
    String modifyUserData(Principal principal,@Validated UserForm form, BindingResult bindingResult,
                          Model model){
        if(bindingResult.hasErrors()){
            Set<String> err=new HashSet<>();
            bindingResult.getAllErrors().forEach(e->err.add(e.getDefaultMessage()));
            model.addAttribute("errors",err);
            //return modifyUserDataPage(model);
            return "mypage"; //多分上と同義
        }

        try {
            User newUser = userService.find(Util.getUserFromPrincipal(principal).getUserId());
            if (!Objects.equals(form.getScreenName(), ""))
                newUser.setScreenName(form.getScreenName());
            if (!Objects.equals(form.getBiography(), ""))
                newUser.setBiography(form.getBiography());
            userService.update(newUser);

            Util.updateAuthenticate((Authentication) principal, newUser);

            model.addAttribute("userinfo",newUser);
        }catch (UserIdNotFoundException e){
            Set<String> errors=new HashSet<>();
            errors.add(e.getMessage());
            model.addAttribute("errors",errors);
            return "mypage";
        }catch (Exception e){
            Set<String> errors=new HashSet<>();
            errors.add("unexpected error occured. try again.");
            model.addAttribute("errors",errors);
            log.info(e.getMessage());
            return "mypage";
        }
        return "redirect:/";

    }




    @GetMapping("/debug")
    String debug(){
        List<User> users=userService.findAll();
        for (User u:users){
            log.info(u.toString());
        }



        return "redirect:/";
    }
}
