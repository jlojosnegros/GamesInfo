package LosOdiosos3.prueba_servidor.Application;



import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class adminController {
	
	// repositorio de la tabla compañias
			

			// repositorio de la tabla juegos
			@Autowired
			private GameRepository gameRepository;
			// ----------------------------- FIN INYECCIONES ----------------------------------
	
		@RequestMapping("/admin")
		public String addGame (Model model, HttpSession usuario, HttpServletRequest request) {
			
			List<Game> games=gameRepository.findAll();
			List<String>listGames=new ArrayList<String>();
			
			
			
		
			for(Game g:games) {
				String name=g.getName();
				
				String aux=String.format("<option value=\"%s\">%s</option>", name,name);
				listGames.add(aux);
			}
			
			model.addAttribute("listGames", listGames);
			
		fillModel(model,usuario,request);
			
			return "admin";
			
		}
		
		public void fillModel(Model model,HttpSession usuario,HttpServletRequest request) {
			
			
			// se pasan los atributos de la barra de navegacion
			model.addAttribute("registered", usuario.getAttribute("registered"));
			boolean aux = !(Boolean) usuario.getAttribute("registered");
			model.addAttribute("unregistered", aux);
			model.addAttribute("name", usuario.getAttribute("name"));
			model.addAttribute("profile_img",String.format("<img src=\"%s\" class=\"profile_img\">",(String) usuario.getAttribute("icon")));

			model.addAttribute("alert"," ");
			model.addAttribute("hello", " ");
			model.addAttribute("Titulo", " ");
			model.addAttribute("Cuerpo", " ");	
			
			// atributos del token
			CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
			model.addAttribute("token", token.getToken());
			
		}
	}

