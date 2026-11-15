function before(hook, param)
    local uri = param:getArgument(0)
    if uri == nil then
        return false
    end

    local authority = uri:getAuthority()

    if 'call_log' or authority == 'call_log_shadow'  then

            local prefix = string.gmatch(path, '[^/]+')()
            if prefix == 'provider_status' then
                return false
            end

        return false
    end
end