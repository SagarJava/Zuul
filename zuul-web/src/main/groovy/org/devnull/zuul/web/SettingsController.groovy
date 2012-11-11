package org.devnull.zuul.web

import org.devnull.util.pagination.HttpRequestPagination
import org.devnull.zuul.data.model.SettingsEntry
import org.devnull.zuul.service.ZuulService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

import org.springframework.web.bind.annotation.*

@Controller
class SettingsController {
    final def log = LoggerFactory.getLogger(this.class)

    @Autowired
    ZuulService zuulService

    /**
     * Start the workflow for creating a new settings group
     * @return
     */
    @RequestMapping(value = "/settings/create", method = RequestMethod.GET)
    String newSettingsGroupForm() {
        return "/settings/create"
    }

    /**
     *  Begin a new settings group with no key/vales.
     */
    @RequestMapping(value = "/settings/create/scratch")
    String createFromScratch(@RequestParam("name") String name, @RequestParam("environment") String env) {
        def settingsGroup = zuulService.createEmptySettingsGroup(name, env)
        return "redirect:/settings/${settingsGroup.name}#${settingsGroup.environment.name}"
    }

    /**
     * Display form for creating new settings groups from property file uploads
     */
    @RequestMapping(value = "/settings/create/upload", method = RequestMethod.GET)
    public ModelAndView createFromUpload(@RequestParam("name") String name, @RequestParam("environment") String env) {
        def model = [environment: env, groupName: name]
        return new ModelAndView("/settings/upload", model)
    }

    /**
     * Display form for creating new settings groups from property file uploads
     */
    @RequestMapping(value = "/settings/create/copy", method = RequestMethod.GET)
    public ModelAndView createFromCopy(@RequestParam("name") String name, @RequestParam("environment") String env) {
        def model = [environment: env, groupName: name]
        return new ModelAndView("/settings/copy", model)
    }

    /**
     * Create the new group from the submitted copy
     */
    @RequestMapping(value = "/settings/create/copy", method = RequestMethod.POST)
    public String createFromCopySubmit(@RequestParam("name") String name, @RequestParam("environment") String env,
                                       @RequestParam("search") String search) {
        def match = search =~ /\/(.+)\/(.+)\.properties$/
        if (match) {
            def group = zuulService.findSettingsGroupByNameAndEnvironment(match.group(2), match.group(1))
            zuulService.createSettingsGroupFromCopy(name, env, group)
        }
        // TODO needs some error handling
        return "redirect:/settings/${name}#${env}"
    }

    /**
     * Show the form for a new key/value entry for the settings group
     */
    @RequestMapping(value = "/settings/{environment}/{name}/create/entry", method = RequestMethod.GET)
    ModelAndView addEntryForm(@PathVariable("name") String groupName, @PathVariable("environment") String environment) {
        def group = zuulService.findSettingsGroupByNameAndEnvironment(groupName, environment)
        return new ModelAndView("/settings/entry", [group: group])
    }

    /**
     * Create a new key/value entry for the settings group
     */
    @RequestMapping(value = "/settings/{environment}/{groupName}/create/entry", method = RequestMethod.POST)
    ModelAndView addEntrySubmit(@PathVariable("groupName") String groupName, @PathVariable("environment") String env,
                                @ModelAttribute("formEntry") @Valid SettingsEntry formEntry, BindingResult result) {
        if (result.hasErrors()) {
            return addEntryForm(groupName, env)
        }
        zuulService.save(formEntry)
        return new ModelAndView("redirect:/settings/${groupName}#${env}")
    }

    /**
     * User interface for editing settings group
     */
    @RequestMapping(value = "/settings/{name}", method = RequestMethod.GET)
    ModelAndView show(@PathVariable("name") String name) {
        def environments = zuulService.listEnvironments()
        def groupsByEnv = [:]
        environments.each { env ->
            groupsByEnv[env] = zuulService.findSettingsGroupByNameAndEnvironment(name, env.name)
        }
        def model = [groupsByEnv: groupsByEnv, groupName: name, environments: environments]
        return new ModelAndView("/settings/show", model)
    }


    @RequestMapping(value = "/settings/{environment}/{groupName}/key/change", method = RequestMethod.GET)
    String changeGroupKey(@PathVariable("environment") String environment, @PathVariable("groupName") String groupName,
                          @RequestParam String keyName) {
        def group = zuulService.findSettingsGroupByNameAndEnvironment(groupName, environment)
        def key = zuulService.findKeyByName(keyName)
        zuulService.changeKey(group, key)
        return "redirect:/settings/${groupName}#${environment}"
    }


    @RequestMapping(value = "/settings/search")
    ModelAndView search(@RequestParam("q") String query, HttpServletRequest request) {
        def pagination = new HttpRequestPagination<SettingsEntry>(request)
        def results = zuulService.search(query, pagination)?.groupBy { it.group }
        return new ModelAndView("/settings/search", [query: query, results: results])
    }
}
