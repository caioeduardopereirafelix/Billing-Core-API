package billing_core_api.domain.mapper;

import billing_core_api.dto.user.ResponseUserDTO;
import billing_core_api.domain.user.User;
import billing_core_api.dto.user.CreateUserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mapping(source = "name", target = "name")
    @Mapping(source = "email", target = "email")
    @Mapping(target = "password", ignore = true)
    User toUser(CreateUserDTO dto);

    CreateUserDTO toUserDto(User user);


    ResponseUserDTO toUserResponse (User dto);
}
