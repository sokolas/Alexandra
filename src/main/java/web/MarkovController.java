package web;

import markov.MarkovService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@RestController
public class MarkovController {
    @Autowired
    private MarkovService markovService;

    @RequestMapping(value="/", method = RequestMethod.GET)
    public String list() {
        List<String> users = markovService.getUsers();
        users.add(0, "all");
        StringBuilder sb = new StringBuilder("<html><body>\n");
        for (String user: users) {
            String s = HtmlUtils.htmlEscape(user);
            sb.append("<a href=\"").append(s).append("\">").append(s).append("</a><br>\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    @RequestMapping(value="/{name}", method = RequestMethod.GET)
    public String quote(@PathVariable String name) {
        String data = "";
        if (!StringUtils.hasText(name)) {
            data = "Not found";
        } else {
            if (name.equals("all")) {
                data = "(все): " + HtmlUtils.htmlEscape(markovService.generateForAll());
            } else {
                data = "(" + name + "): " + HtmlUtils.htmlEscape(markovService.generateForName(name));
            }
        }
        return "<html><body>" + data + "</body></html>";
    }
}
